package uk.ac.ebi.biosamples.service;

import java.util.Iterator;
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
import uk.ac.ebi.biosamples.mongo.model.MongoSampleTab;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleTabRepository;

@Service
public class SampleTabIdService {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private MongoSampleTabRepository mongoSampleRepository;

	private BlockingQueue<String> accessionCandidateQueue;;
	private long accessionCandidateCounter;
	
	@Autowired
	private MongoProperties mongoProperties;

	@PostConstruct
	public void doSetup() {
		accessionCandidateQueue = new LinkedBlockingQueue<>(mongoProperties.getAcessionQueueSize());
		accessionCandidateCounter = mongoProperties.getAccessionMinimum();
	}
	
	public MongoSampleTab accessionAndInsert(MongoSampleTab sample) {
		log.trace("generating an accession");
		// inspired by Optimistic Loops of
		// https://docs.mongodb.com/v3.0/tutorial/create-an-auto-incrementing-field/
		boolean success = false;
		// TODO limit number of tries
		while (!success) {
			// TODO add a timeout here
			try {
				sample = MongoSampleTab.build(accessionCandidateQueue.take(), sample.getDomain(), sample.getSampleTab(), sample.getAccessions());
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			try {
				sample = mongoSampleRepository.insertNew(sample);
				success = true;
			} catch (MongoWriteException e) {
				if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
					success = false;
					sample = MongoSampleTab.build(null, sample.getDomain(), sample.getSampleTab(), sample.getAccessions());
				} else {
					throw e;
				}
			}
		}
		log.debug("generated accession "+sample);
		return sample;
	}
	
	@Scheduled(fixedDelay = 1000)
	public void prepareAccessions() {	
		//check that all accessions are still available		
		Iterator<String> it = accessionCandidateQueue.iterator();
		while (it.hasNext()) {
			String accessionCandidate = it.next();
			MongoSampleTab sample = mongoSampleRepository.findOne(accessionCandidate);
			if (sample != null) {
				log.warn("Removing accession "+accessionCandidate+" from queue because now assigned");
				it.remove();
			}
		}
		
		while (accessionCandidateQueue.remainingCapacity() > 0) {
			log.debug("Adding more accessions to queue");
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
			log.trace("Added more accessions to queue");
		}
	}

}
