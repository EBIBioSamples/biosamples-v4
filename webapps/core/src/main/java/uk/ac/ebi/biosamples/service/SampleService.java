package uk.ac.ebi.biosamples.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.SolrResultPage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;

import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.WebappProperties;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoSubmission;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoSubmissionRepository;
import uk.ac.ebi.biosamples.solr.model.SampleFacets;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;

/**
 * Service layer business logic for centralising repository access and
 * conversions between different controllers. Use this instead of linking to
 * repositories directly.
 * 
 * @author faulcon
 *
 */
@Service
public class SampleService {

	@Autowired
	private MongoSampleRepository mongoSampleRepository;
	@Autowired
	private MongoSubmissionRepository mongoSubmissionRepository;
	
	@Autowired
	private InverseRelationshipService inverseRelationshipService;
	
	@Autowired
	private SolrSampleService solrSampleService;

	@Autowired
	private AmqpTemplate amqpTemplate;
	
	@Autowired
	private ConversionService conversionService;
	
	@Autowired
	private WebappProperties webappProperties;

	private BlockingQueue<String> accessionCandidateQueue;;
	private long accessionCandidateCounter;

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@PostConstruct
	public void doSetup() {
		accessionCandidateQueue = new LinkedBlockingQueue<>(webappProperties.getAcessionQueueSize());
		accessionCandidateCounter = webappProperties.getAccessionMinimum();
	}

	/**
	 * Throws an IllegalArgumentException of no sample with that accession exists
	 * 
	 * @param accession
	 * @return
	 * @throws IllegalArgumentException
	 */
	public Sample fetch(String accession) throws IllegalArgumentException {
		// return the raw sample from the repository
		MongoSample mongoSample = mongoSampleRepository.findOne(accession);
		if (mongoSample == null) {
			throw new IllegalArgumentException("Unable to find sample (" + accession + ")");
		}

		// convert it into the format to return
		Sample sample = conversionService.convert(mongoSample, Sample.class);
		
		// add any additional inverse relationships
		sample = inverseRelationshipService.insertInverses(sample);
		
		//TODO only have relationships to things that are present
		
		return sample;
	}
	
	public Page<Sample> getSamplesByText(String text, MultiValueMap<String,String> filters, Pageable pageable) {
		Page<SolrSample> pageSolrSample = solrSampleService.fetchSolrSampleByText(text, filters, pageable);
		// for each result fetch the version from Mongo and add inverse relationships
		Page<Sample> pageSample = pageSolrSample.map(ss->fetch(ss.getAccession()));
		return pageSample;
	}
	
	public SampleFacets getFacets(String text, MultiValueMap<String,String> filters, int noOfFacets, int noOfFacetValues) {
		Pageable facetPageable = new PageRequest(0,noOfFacets);
		Pageable facetValuePageable = new PageRequest(0,noOfFacetValues);
		return solrSampleService.getFacets(text, filters, facetPageable, facetValuePageable);
	}
	
	public MultiValueMap<String,String> getFilters(String[] filterStrings) {
		if (filterStrings == null) return null;
		if (filterStrings.length == 0) return null;
		//sort the array
		Arrays.sort(filterStrings);
		SortedSet<String> filterStringSet = new TreeSet<>(Arrays.asList(filterStrings));
		//strip the requestParams down to just the selected facet information
		MultiValueMap<String,String> filters = new LinkedMultiValueMap<>();
		for (String filterString : filterStringSet) {
			if (filterString.contains(":")) {
				String key = filterString.substring(0, filterString.indexOf(":"));
				String value = filterString.substring(filterString.indexOf(":")+1, filterString.length());
				//key = SolrSampleService.attributeTypeToField(key);
				filters.add(key, value);
				log.info("adding filter "+key+" = "+value);
			} else {
				String key=filterString;
				//key = SolrSampleService.attributeTypeToField(key);
				filters.add(key, null);
				log.info("adding filter "+key);
			}
		}
		return filters;
	}

	public Sample store(Sample sample) {
		// TODO check if there is an existing copy and if there are any changes
		
		// save the submission in the repository
		mongoSubmissionRepository.save(new MongoSubmission(sample, LocalDateTime.now()));

		// TODO validate that relationships have this sample as the source 

		// convert it to the storage specific version
		MongoSample mongoSample = conversionService.convert(sample, MongoSample.class);
		// save the sample in the repository
		if (mongoSample.hasAccession()) {
			//update the existing accession
			mongoSampleRepository.save(mongoSample);
		} else {
			//TODO see if there is an existing accession for this user and name
						
			//assign it a new accession
			mongoSample = accessionAndInsert(mongoSample);
			//update the sample object with the assigned accession
			sample = Sample.build(sample.getName(), mongoSample.getAccession(), sample.getRelease(), sample.getUpdate(),
					sample.getAttributes(), sample.getRelationships(), sample.getExternalReferences());
		}
		// send a message for further processing
		amqpTemplate.convertAndSend(Messaging.exchangeForIndexing, "", sample);
		//return the sample in case we have modified it i.e accessioned
		return sample;
	}

	private MongoSample accessionAndInsert(MongoSample sample) {
		// inspired by Optimistic Loops of
		// https://docs.mongodb.com/v3.0/tutorial/create-an-auto-incrementing-field/
		boolean success = false;
		// TODO limit number of tries
		while (!success) {
			// TODO add a timeout here
			try {
				sample.accession = accessionCandidateQueue.take();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			try {
				sample = mongoSampleRepository.insertNew(sample);
				success = true;
			} catch (MongoWriteException e) {
				if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
					success = false;
					sample.accession = null;
				} else {
					throw e;
				}
			}
		}
		return sample;
	}

	@Scheduled(fixedDelay = 100)
	public void prepareAccessions() {
		while (accessionCandidateQueue.remainingCapacity() > 0) {
			String accessionCandidate = webappProperties.getAccessionPrefix() + accessionCandidateCounter;
			// if the accession already exists, skip it
			if (mongoSampleRepository.exists(accessionCandidate)) {
				accessionCandidateCounter += 1;
				// if the accession can't be put in the queue at this time
				// (queue full), stop
			} else if (!accessionCandidateQueue.offer(accessionCandidate)) {
				return;
			} else {
				//put it into the queue, move on to next
				accessionCandidateCounter += 1;
			}
		}
	}
}
