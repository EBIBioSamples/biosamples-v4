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
	private MongoSampleTabRepository mongoSampleTabRepository;

	private BlockingQueue<String> accessionCandidateQueue;;
	private long accessionCandidateCounter;
	
	@Autowired
	private MongoProperties mongoProperties;

	@PostConstruct
	public void doSetup() {
		accessionCandidateQueue = new LinkedBlockingQueue<>(mongoProperties.getAcessionQueueSize());
		accessionCandidateCounter = 1;
	}
	
	public MongoSampleTab accessionAndInsert(MongoSampleTab mongoSampleTab) {
		log.trace("generating an accession");
		// inspired by Optimistic Loops of
		// https://docs.mongodb.com/v3.0/tutorial/create-an-auto-incrementing-field/
		boolean success = false;
		// TODO limit number of tries
		while (!success) {
			// TODO add a timeout here
			try {
				mongoSampleTab = MongoSampleTab.build(accessionCandidateQueue.take(), mongoSampleTab.getDomain(), mongoSampleTab.getSampleTab(), mongoSampleTab.getAccessions());
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			try {
				mongoSampleTab = mongoSampleTabRepository.insertNew(mongoSampleTab);
				success = true;
			} catch (MongoWriteException e) {
				if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
					success = false;
					mongoSampleTab = MongoSampleTab.build(null, mongoSampleTab.getDomain(), mongoSampleTab.getSampleTab(), mongoSampleTab.getAccessions());
				} else {
					throw e;
				}
			}
		}
		log.debug("generated id "+mongoSampleTab.getId());
		return mongoSampleTab;
	}
	
	@Scheduled(fixedDelay = 1000)
	public void prepareAccessions() {	
		//check that all accessions are still available		
		Iterator<String> it = accessionCandidateQueue.iterator();
		while (it.hasNext()) {
			String accessionCandidate = it.next();
			MongoSampleTab sample = mongoSampleTabRepository.findOne(accessionCandidate);
			if (sample != null) {
				log.warn("Removing accession "+accessionCandidate+" from queue because now assigned");
				it.remove();
			}
		}
		
		while (accessionCandidateQueue.remainingCapacity() > 0) {
			log.debug("Adding more accessions to queue");
			String accessionCandidate = "GSB-" + accessionCandidateCounter;
			// if the accession already exists, skip it
			if (mongoSampleTabRepository.exists(accessionCandidate)) {
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
