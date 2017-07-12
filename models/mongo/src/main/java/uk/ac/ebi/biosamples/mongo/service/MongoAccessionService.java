package uk.ac.ebi.biosamples.mongo.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;

import uk.ac.ebi.biosamples.mongo.MongoProperties;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;

@Service
public class MongoAccessionService {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private MongoSampleRepository mongoSampleRepository;

	private BlockingQueue<String> accessionCandidateQueue;;
	private long accessionCandidateCounter;
	
	@Autowired
	private MongoProperties mongoProperties;

	@PostConstruct
	public void doSetup() {
		accessionCandidateQueue = new LinkedBlockingQueue<>(mongoProperties.getAcessionQueueSize());
		accessionCandidateCounter = mongoProperties.getAccessionMinimum();
	}

	public MongoSample accessionAndInsert(MongoSample sample) {
		log.info("generating an accession");
		// inspired by Optimistic Loops of
		// https://docs.mongodb.com/v3.0/tutorial/create-an-auto-incrementing-field/
		boolean success = false;
		// TODO limit number of tries
		while (!success) {
			// TODO add a timeout here
			try {
				sample = MongoSample.build(sample.getName(), accessionCandidateQueue.take(), 
						sample.getRelease(), sample.getUpdate(), sample.getAttributes(), sample.getRelationships(), sample.getExternalReferences());
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			try {
				sample = mongoSampleRepository.insertNew(sample);
				success = true;
			} catch (MongoWriteException e) {
				if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
					success = false;
					sample = MongoSample.build(sample.getName(), null, 
							sample.getRelease(), sample.getUpdate(), sample.getAttributes(), sample.getRelationships(), sample.getExternalReferences());
				} else {
					throw e;
				}
			}
		}
		log.info("generated accession "+sample);
		return sample;
	}

	@Scheduled(fixedDelay = 100)
	public void prepareAccessions() {
		while (accessionCandidateQueue.remainingCapacity() > 0) {
			log.info("Adding more accessions to queue");
			String accessionCandidate = mongoProperties.getAccessionPrefix() + accessionCandidateCounter;
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
			log.info("Added more accessions to queue");
		}
	}
}
