package uk.ac.ebi.biosamples.service;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import uk.ac.ebi.biosamples.neo.NeoProperties;
import uk.ac.ebi.biosamples.neo.model.NeoSample;
import uk.ac.ebi.biosamples.neo.repo.NeoSampleRepository;

/**
 * Do not declare an explicit bean, rely on users to create bean in config
 * 
 * 
 * @author faulcon
 *
 */
public class NeoAccessionService {

	private Logger log = LoggerFactory.getLogger(getClass());

	private NeoSampleRepository neoSampleRepository;
	
	private NeoProperties neoProperties;

	private BlockingQueue<String> accessionCandidateQueue;;
	private long accessionCandidateCounter;
	
	
	public NeoAccessionService(NeoSampleRepository neoSampleRepository, NeoProperties neoProperties) {
		this.neoSampleRepository = neoSampleRepository;
		this.neoProperties = neoProperties;
	}

	@PostConstruct
	public void doSetup() {
		accessionCandidateQueue = new LinkedBlockingQueue<>(neoProperties.getAcessionQueueSize());
		accessionCandidateCounter = neoProperties.getAccessionMinimum();
	}

	public String generateAccession() {
		log.info("generating an accession");
		
		// inspired by Optimistic Loops of
		// https://docs.mongodb.com/v3.0/tutorial/create-an-auto-incrementing-field/
		String accessionCandidate = null;
		// TODO limit number of tries
		while (accessionCandidate == null) {
			// TODO add a timeout here
			try {
				accessionCandidate = accessionCandidateQueue.take();
			} catch (InterruptedException e1) {
				throw new RuntimeException(e1);
			}
			
			log.info("testing accession "+accessionCandidate);
			
			try {
				neoSampleRepository.testNewAccession(accessionCandidate);
			} catch (DataIntegrityViolationException e) {
				accessionCandidate = null;
			}
		}
		log.info("generated accession "+accessionCandidate);
		return accessionCandidate;
	}

	@Scheduled(fixedDelay = 1000)
	public void prepareAccessions() {
		
		//check that all accessions are still available		
		Iterator<String> it = accessionCandidateQueue.iterator();
		while (it.hasNext()) {
			String accessionCandidate = it.next();
			NeoSample sample = neoSampleRepository.findOneByAccession(accessionCandidate, 0);
			if (sample != null) {
				log.warn("Removing accession "+accessionCandidate+" from queue because now assigned");
				it.remove();
			}
		}
		
		//make sure that the queue is full of potential candidates
		while (accessionCandidateQueue.remainingCapacity() > 0) {
			log.info("Adding more accessions to queue");
			String accessionCandidate = neoProperties.getAccessionPrefix() + accessionCandidateCounter;
			if (neoSampleRepository.findOneByAccession(accessionCandidate,0) != null) {
				// if the accession already exists, skip it
				accessionCandidateCounter += 1;
			} else if (!accessionCandidateQueue.offer(accessionCandidate)) {
				// if the accession can't be put in the queue at this time
				// (queue full), stop
				return;
			} else {
				//put it into the queue, move on to next
				accessionCandidateCounter += 1;
			}
			log.info("Added more accessions to queue");
		}
	}
}
