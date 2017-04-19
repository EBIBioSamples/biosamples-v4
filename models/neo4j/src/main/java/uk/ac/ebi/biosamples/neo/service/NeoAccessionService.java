package uk.ac.ebi.biosamples.neo.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.neo.NeoProperties;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;

@Service
public class NeoAccessionService {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private NeoSampleRepository neoSampleRepository;

	private BlockingQueue<String> accessionCandidateQueue;;
	private long accessionCandidateCounter;
	
	@Autowired
	private NeoProperties neoProperties;

	@PostConstruct
	public void doSetup() {
		accessionCandidateQueue = new LinkedBlockingQueue<>(neoProperties.getAcessionQueueSize());
		accessionCandidateCounter = neoProperties.getAccessionMinimum();
	}

	public NeoSample accessionAndInsert(NeoSample sample) {
		// inspired by Optimistic Loops of
		// https://docs.mongodb.com/v3.0/tutorial/create-an-auto-incrementing-field/
		boolean success = false;
		// TODO limit number of tries
		while (!success) {
			// TODO add a timeout here
			try {
				sample = NeoSample.build(sample.getName(), accessionCandidateQueue.take(), 
						sample.getRelease(), sample.getUpdate(), sample.getAttributes(), sample.getRelationships(), sample.getExternalReferences());
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			try {
				sample = neoSampleRepository.insertNew(sample);
				success = true;
			} catch (DataIntegrityViolationException e) {
				success = false;
				sample = NeoSample.build(sample.getName(), null, 
						sample.getRelease(), sample.getUpdate(), sample.getAttributes(), sample.getRelationships(), sample.getExternalReferences());
			}
			
		}
		return sample;
	}

	@Scheduled(fixedDelay = 100)
	public void prepareAccessions() {
		while (accessionCandidateQueue.remainingCapacity() > 0) {
			String accessionCandidate = neoProperties.getAccessionPrefix() + accessionCandidateCounter;
			// if the accession already exists, skip it
			if (neoSampleRepository.findOneByAccession(accessionCandidate) != null) {
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
