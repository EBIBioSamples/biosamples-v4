package uk.ac.ebi.biosamples.neo.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import uk.ac.ebi.biosamples.neo.NeoProperties;
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
			try {
				neoSampleRepository.testNewAccession(accessionCandidate);
			} catch (DataIntegrityViolationException e) {
				accessionCandidate = null;
			}
		}
		return accessionCandidate;
	}

	@Scheduled(fixedDelay = 100)
	public void prepareAccessions() {
		while (accessionCandidateQueue.remainingCapacity() > 0) {
			String accessionCandidate = neoProperties.getAccessionPrefix() + accessionCandidateCounter;
			// if the accession already exists, skip it
			if (neoSampleRepository.findOneByAccession(accessionCandidate,0) != null) {
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
