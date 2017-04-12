package uk.ac.ebi.biosamples.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;

import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.WebappProperties;
import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SampleFacets;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoSubmission;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoSubmissionRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
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

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private MongoSampleRepository mongoSampleRepository;
	@Autowired
	private MongoSubmissionRepository mongoSubmissionRepository;
	
	@Autowired
	private MongoAccessionService mongoAccessionService;
	
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
	
	@Async("threadPoolTaskExecutor")
	public Future<Sample> fetchAsync(String accession) {
		return new AsyncResult<>(fetch(accession));
	}
	
	//does this asynchonously
	public Page<Sample> getSamplesByText(String text, MultiValueMap<String,String> filters, Pageable pageable) {
		Page<SolrSample> pageSolrSample = solrSampleService.fetchSolrSampleByText(text, filters, pageable);
		// for each result fetch the version from Mongo and add inverse relationships
		//Page<Sample> pageSample = pageSolrSample.map(ss->fetch(ss.getAccession()));
		
		List<Future<Sample>> futures = new ArrayList<>();
		for (SolrSample solrSample : pageSolrSample) {
			futures.add(fetchAsync(solrSample.getAccession()));
		}
		List<Sample> samples = new ArrayList<>();
		for (Future<Sample> future : futures) {
			Sample sample = null;
			try {
				sample = future.get();
			} catch (InterruptedException | ExecutionException e) {
				//TODO handle better
				throw new RuntimeException(e);
			}
			samples.add(sample);
		}		
		Page<Sample> pageSample = new PageImpl<>(samples,pageable, pageSolrSample.getTotalElements());
		return pageSample;
	}
	
	public SampleFacets getFacets(String text, MultiValueMap<String,String> filters, int noOfFacets, int noOfFacetValues) {
		Pageable facetPageable = new PageRequest(0,noOfFacets);
		Pageable facetValuePageable = new PageRequest(0,noOfFacetValues);
		//TODO if a facet is enabled as a filter, then that value will be the only filter displayed
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
			log.info("looking at filter string '"+filterString+"'");
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
	
	public Autocomplete getAutocomplete(String autocompletePrefix, MultiValueMap<String,String> filters, int noSuggestions) {
		return solrSampleService.getAutocomplete(autocompletePrefix, filters, noSuggestions);
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
			mongoSample = mongoAccessionService.accessionAndInsert(mongoSample);
			//update the sample object with the assigned accession
			sample = Sample.build(sample.getName(), mongoSample.getAccession(), sample.getRelease(), sample.getUpdate(),
					sample.getAttributes(), sample.getRelationships(), sample.getExternalReferences());
		}
		// send a message for further processing
		amqpTemplate.convertAndSend(Messaging.exchangeForIndexing, "", sample);
		//return the sample in case we have modified it i.e accessioned
		return sample;
	}

}
