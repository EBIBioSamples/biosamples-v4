package uk.ac.ebi.biosamples.service;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;

import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.WebappProperties;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoSubmission;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoSubmissionRepository;
import uk.ac.ebi.biosamples.solr.model.SolrSample;
import uk.ac.ebi.biosamples.solr.repo.SolrSampleRepository;

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
	private SolrSampleRepository solrSampleRepository;

	//this has the same in and out class, so can't be used via conversionservice
	@Autowired
	private InverseRelationshipConverter inverseRelationshipConverter;

	@Autowired
	private AmqpTemplate amqpTemplate;
	
	@Autowired
	private ConversionService conversionService;
	
	@Autowired
	private WebappProperties webappProperties;

	private BlockingQueue<String> accessionCandidateQueue;;
	private long accessionCandidateCounter;
	
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
		sample = inverseRelationshipConverter.convert(sample);
		
		return sample;
	}

	public Page<Sample> fetchFindAll(Pageable pageable) {
		// return the samples from solr that match the query
		Page<SolrSample> pageSolrSample = solrSampleRepository.findPublic(pageable);
		// for each result fetch the version from Mongo and add inverse relationships
		Page<Sample> pageSample = pageSolrSample.map(s -> fetch(s.getAccession()));
		return pageSample;
	}

	public Page<Sample> fetchFindByText(String text, Pageable pageable) {
		// return the samples from solr that match the query
		Page<SolrSample> pageSolrSample = solrSampleRepository.findByTextAndPublic(text, pageable);
		// for each result fetch the version from Mongo and add inverse relationships
		Page<Sample> pageSample = pageSolrSample.map(s -> fetch(s.getAccession()));
		return pageSample;
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
					sample.getAttributes(), sample.getRelationships());
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
