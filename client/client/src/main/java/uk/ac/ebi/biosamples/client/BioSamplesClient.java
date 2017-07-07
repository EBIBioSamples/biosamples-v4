package uk.ac.ebi.biosamples.client;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.biosamples.client.service.AapClientService;
import uk.ac.ebi.biosamples.client.service.CurationSubmissionService;
import uk.ac.ebi.biosamples.client.service.SampleRetrievalService;
import uk.ac.ebi.biosamples.client.service.SampleSubmissionService;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleValidator;

/**
 * This is the primary class for interacting with BioSamples.
 * 
 * It has Spring annotations for autowiring into spring applications
 * 
 * @author faulcon
 *
 */
public class BioSamplesClient {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final SampleRetrievalService sampleRetrievalService;
	private final SampleSubmissionService sampleSubmissionService;
	private final CurationSubmissionService curationSubmissionService;
	
	private final SampleValidator sampleValidator;
	
	private final ExecutorService threadPoolExecutor;
	
	public BioSamplesClient(URI uri, RestTemplateBuilder restTemplateBuilder, 
			SampleValidator sampleValidator, AapClientService aapClientService) {
		//TODO application.properties this
		threadPoolExecutor = Executors.newFixedThreadPool(64);
		
		RestTemplate restOperations = restTemplateBuilder.build();
				
		if (aapClientService != null) {		
			restOperations.getInterceptors().add(new AapClientHttpRequestInterceptor(aapClientService));
		}
		
		Traverson traverson = new Traverson(uri, MediaTypes.HAL_JSON);
		traverson.setRestOperations(restOperations);
		
		sampleRetrievalService = new SampleRetrievalService(restOperations, traverson, threadPoolExecutor);
		sampleSubmissionService = new SampleSubmissionService(restOperations, traverson, threadPoolExecutor);
		curationSubmissionService = new CurationSubmissionService(restOperations, traverson, threadPoolExecutor);
		
		this.sampleValidator = sampleValidator;
	}

	private static class AapClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
		
		private final AapClientService aapClientService;

		public AapClientHttpRequestInterceptor(AapClientService aapClientService) {
			this.aapClientService = aapClientService;
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
				throws IOException {			
			if (aapClientService != null && !request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
				String jwt = aapClientService.getJwt();
				request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer "+jwt);
			}

			//pass along to the next interceptor
			return execution.execute(request, body);
		}
		
	}
    
    @PreDestroy
    public void close() {
    	if (threadPoolExecutor != null) {
    		try {
	    		threadPoolExecutor.shutdown();
				if (!threadPoolExecutor.awaitTermination(1, TimeUnit.MINUTES)) {
		    		threadPoolExecutor.shutdownNow();
				}
			} catch (InterruptedException e) {
				//don't care about being interrupted here?
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
	
	public Optional<Sample> fetchSample(String accession) throws RestClientException {
		Optional<Resource<Sample>> resource = fetchSampleResource(accession);
		if (resource.isPresent()) {
			return Optional.of(resource.get().getContent());
		} else {
			return Optional.empty();
		}
	}	

	public Iterable<Resource<Sample>> fetchSampleResourceAll() throws RestClientException {
		return sampleRetrievalService.fetchAll();
	}

	public Iterable<Optional<Resource<Sample>>> fetchSampleResourceAll(Iterable<String> accessions) throws RestClientException {
		return sampleRetrievalService.fetchAll(accessions);
	}

	public Resource<Sample> persistSampleResource(Sample sample) throws RestClientException {
		if (sampleValidator != null) {
			//validate client-side before submission
			Collection<String> errors = sampleValidator.validate(sample);		
			if (errors.size() > 0) {
				log.info("Errors : "+errors);
				throw new IllegalArgumentException("Sample not valid");
			}
		}
		
		try {
			return sampleSubmissionService.submitAsync(sample).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}
	
	public Sample persistSample(Sample sample) throws RestClientException {
		return persistSampleResource(sample).getContent();
	}
	
	public Collection<Resource<Sample>> persistSamples(Collection<Sample> samples) throws RestClientException {
		List<Resource<Sample>> results = new ArrayList<>();
		List<Future<Resource<Sample>>> futures = new ArrayList<>();
		
		for (Sample sample : samples) {
			futures.add(sampleSubmissionService.submitAsync(sample));
		}
		
		for (Future<Resource<Sample>> future : futures) {
			Resource<Sample> sample;

			try {
				sample = future.get();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				throw new RuntimeException(e.getCause());
			}
			results.add(sample);
		}
		return results;
	}
        
    @Deprecated
	public Resource<Sample> fetchResource(String accession) {
		try {
			//TODO add timeout?
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
	
	public Resource<CurationLink> persistCuration(CurationLink curationLink) throws RestClientException {
		return curationSubmissionService.submit(curationLink);
	}
	
	@Deprecated
	public Sample fetch(String accession) {
		return fetchResource(accession).getContent();
	}	

	@Deprecated
	public Resource<Sample> persistResource(Sample sample) {
		try {
			//TODO add timeout?
			return sampleSubmissionService.submitAsync(sample).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}
	@Deprecated
	public Sample persist(Sample sample) {
		return persistResource(sample).getContent();
	}

	public PagedResources<Resource<Sample>> fetchPagedSamples(String text, int startPage, int size) {
		return sampleRetrievalService.fetchPaginated(text, startPage, size);
	}
}
