package uk.ac.ebi.biosamples.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;

import uk.ac.ebi.biosamples.Messaging;
import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoSubmission;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoSubmissionRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
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

	@Autowired
	private InverseRelationshipService inverseRelationshipService;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	private MongoSampleToSampleConverter mongoSampleToSampleConverter;
	@Autowired
	private SampleToMongoSampleConverter sampleToMongoSampleConverter;

	// TODO make this a application property configurable;
	private BlockingQueue<String> accessionCandidateQueue = new LinkedBlockingQueue<>(100);
	private String accessionCandidatePrefix = "TSTE";
	private long accessionCandidateCounter = 1000000;

	public Sample fetch(String accession) {
		// return the raw sample from the repository
		MongoSample mongoSample = mongoSampleRepository.findOne(accession);
		if (mongoSample == null) {
			// TODO return a 404 error
			throw new RuntimeException("Unable to find sample (" + accession + ")");
		}

		// add any additional inverse relationships
		inverseRelationshipService.addInverseRelationships(mongoSample);

		// convert it into the format to return
		Sample sample = mongoSampleToSampleConverter.convert(mongoSample);
		return sample;
	}

	public Page<Sample> fetchFindAll(Pageable pageable) {
		// return the raw sample from the repository
		Page<MongoSample> pageMongoSample = mongoSampleRepository.findAll(pageable);
		// add any additional inverse relationships
		for (MongoSample mongoSample : pageMongoSample) {
			inverseRelationshipService.addInverseRelationships(mongoSample);
		}
		// convert it into the format to return
		Page<Sample> pageSample = pageMongoSample.map(mongoSampleToSampleConverter);
		return pageSample;
	}

	public Page<Sample> fetchFindByText(String text, Pageable pageable) {
		// return the raw sample from the repository
		Page<SolrSample> pageSolrSample = solrSampleRepository.findByText(text, pageable);
		// fetch the version from Mongo and add inverse relationships
		Page<Sample> pageSample = pageSolrSample.map(s -> fetch(s.getAccession()));
		return pageSample;
	}

	public Sample store(Sample sample) {

		// TODO validate that relationships have this sample as the source

		// convert it to the storage specific version
		MongoSample mongoSample = sampleToMongoSampleConverter.convert(sample);
		// save the submission in the repository
		mongoSubmissionRepository.save(new MongoSubmission(mongoSample));
		// save the sample in the repository
		if (mongoSample.hasAccession()) {
			//update the existing accession
			mongoSampleRepository.save(mongoSample);
		} else {
			//assign it an accession
			accessionAndInsert(mongoSample);
			//update the sample object with the assigned accession
			sample = Sample.build(sample.getName(), mongoSample.getAccession(), sample.getRelease(), sample.getUpdate(),
					sample.getAttributes(), sample.getRelationships());
		}
		// send a message for further processing
		amqpTemplate.convertAndSend(Messaging.exchangeForIndexing, "", sample);
		//return the sample in case we have modified it i.e accessioned
		return sample;
	}

	private void accessionAndInsert(MongoSample sample) {
		// inspired by Optimistic Loops of
		// https://docs.mongodb.com/v3.0/tutorial/create-an-auto-incrementing-field/
		boolean success = false;
		// TODO limit number of tries
		while (!success) {
			// TODO add a timeout here
			try {
				sample.accession = accessionCandidateQueue.take();
			} catch (InterruptedException e1) {
				throw new RuntimeException(e1);
			}

			try {
				mongoSampleRepository.insertNew(sample);
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
	}

	@Scheduled(fixedDelay = 100)
	public void prepareAccessions() {
		while (accessionCandidateQueue.remainingCapacity() > 0) {
			String accessionCandidate = accessionCandidatePrefix + accessionCandidateCounter;
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
