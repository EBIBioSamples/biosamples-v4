package uk.ac.ebi.biosamples.client;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import javax.annotation.PreDestroy;

import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Traverson;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import uk.ac.ebi.biosamples.client.service.ExternalReferenceSubmissionService;
import uk.ac.ebi.biosamples.client.service.SampleRetrievalService;
import uk.ac.ebi.biosamples.client.service.SampleSubmissionService;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.ExternalReferenceLink;
import uk.ac.ebi.biosamples.model.Sample;

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
	
	private final SampleRetrievalService sampleRetrievalService;
	private final SampleSubmissionService sampleSubmissionService;
	private final ExternalReferenceSubmissionService externalReferenceSubmissionService;
	private final ExecutorService threadPoolExecutor;
	
	public BioSamplesClient(ClientProperties clientProperties, RestOperations restOperations) {
		//TODO application.properties this
		threadPoolExecutor = Executors.newFixedThreadPool(64);
		
		Traverson traverson = new Traverson(clientProperties.getBiosamplesClientUri(), MediaTypes.HAL_JSON);
		//traverson.setRestOperations(restOperations);
		
		sampleRetrievalService = new SampleRetrievalService(clientProperties, restOperations, threadPoolExecutor);
		sampleSubmissionService = new SampleSubmissionService(clientProperties, restOperations, threadPoolExecutor);
		externalReferenceSubmissionService = new ExternalReferenceSubmissionService(restOperations, traverson, threadPoolExecutor);
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
    
	public Optional<Resource<Sample>> fetchSampleResource(String accession) throws RestClientException {
		try {
			return sampleRetrievalService.fetch(accession).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	public Iterable<Optional<Resource<Sample>>> fetchSampleResourceAll(Iterable<String> accessions) throws RestClientException {
		return sampleRetrievalService.fetchAll(accessions);
	}
	
	public Optional<Sample> fetchSample(String accession) throws RestClientException {
		Optional<Resource<Sample>> resource = fetchSampleResource(accession);
		if (resource.isPresent()) {
			return Optional.of(resource.get().getContent());
		} else {
			return Optional.empty();
		}
	}	

	public Resource<Sample> persistSampleResource(Sample sample) throws RestClientException {
		return sampleSubmissionService.submit(sample);
	}
	
	public Sample persistSample(Sample sample) throws RestClientException {
		return persistSampleResource(sample).getContent();
	}
	
	public Resource<ExternalReferenceLink> persistExternalReference(String accession, String url) throws RestClientException {
		return externalReferenceSubmissionService.persistExternalReference(accession, ExternalReference.build(url));
	}
        
    @Deprecated
	public Resource<Sample> fetchResource(String accession) {
		try {
			Optional<Resource<Sample>> optional = sampleRetrievalService.fetch(accession).get();
			if (optional.isPresent()) {
				return optional.get();
			} else {
				return null;
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	@Deprecated
	public Iterable<Resource<Sample>> fetchResourceAll(Iterable<String> accessions) {
		return (Iterable<Resource<Sample>>) StreamSupport.stream(sampleRetrievalService.fetchAll(accessions).spliterator(), false)
				.map(s -> s.isPresent()?s.get():null )
				.iterator();
	}
	
	@Deprecated
	public Sample fetch(String accession) {
		return fetchResource(accession).getContent();
	}	

	@Deprecated
	public Resource<Sample> persistResource(Sample sample) {
		return sampleSubmissionService.submit(sample);
	}
	@Deprecated
	public Sample persist(Sample sample) {
		return persistResource(sample).getContent();
	}

	@Deprecated
	public PagedResources<Resource<Sample>> fetchPagedSamples(String text, int startPage, int size) {
		return sampleRetrievalService.fetchPaginated(text, startPage, size);
	}
}
