package uk.ac.ebi.biosamples.client;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;
import uk.ac.ebi.biosamples.client.service.RetrievalService;
import uk.ac.ebi.biosamples.client.service.SubmissionService;
import uk.ac.ebi.biosamples.model.Sample;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This is the primary class for interacting with BioSamples.
 * 
 * It has Spring annotations for autowiring into spring applications
 * 
 * @author faulcon
 *
 */
@Service
public class BioSamplesClient {
	
	private final RetrievalService retrievalService;
	private final SubmissionService submissionService;
	private final ExecutorService threadPoolExecutor;
	
	public BioSamplesClient(ClientProperties clientProperties, RestOperations restOperations) {
		threadPoolExecutor = Executors.newFixedThreadPool(64);
		//TODO application.properties this
		retrievalService = new RetrievalService(clientProperties, restOperations, threadPoolExecutor);
		submissionService = new SubmissionService(clientProperties, restOperations, threadPoolExecutor);
	}

    
    @PreDestroy
    public void shutdownBioSamplesClientTaskExecutor() {
    	if (threadPoolExecutor != null) {
    		threadPoolExecutor.shutdownNow();
    		try {
				threadPoolExecutor.awaitTermination(1, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
    	}
    }
	
	public Resource<Sample> fetchResource(String accession) {
		try {
			return retrievalService.fetch(accession).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	
	public Iterable<Resource<Sample>> fetchResourceAll(Iterable<String> accessions) {
		return retrievalService.fetchAll(accessions);
	}
	
	public Sample fetch(String accession) {
		return fetchResource(accession).getContent();
	}	

	
	public Resource<Sample> persistResource(Sample sample) {
		return submissionService.submit(sample);
	}
	
	public Sample persist(Sample sample) {
		return persistResource(sample).getContent();
	}


	public PagedResources<Resource<Sample>> fetchPagedSamples(int startPage, int size) {
		return retrievalService.fetchAll(startPage, size);
	}
}
