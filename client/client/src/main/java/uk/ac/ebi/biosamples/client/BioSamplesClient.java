package uk.ac.ebi.biosamples.client;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.client.service.AapClientService;
import uk.ac.ebi.biosamples.client.service.CurationRetrievalService;
import uk.ac.ebi.biosamples.client.service.CurationSubmissionService;
import uk.ac.ebi.biosamples.client.service.SampleCursorRetrievalService;
import uk.ac.ebi.biosamples.client.service.SamplePageRetrievalService;
import uk.ac.ebi.biosamples.client.service.SampleRetrievalService;
import uk.ac.ebi.biosamples.client.service.SampleSubmissionService;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.SampleValidator;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;


/**
 * This is the primary class for interacting with BioSamples.
 *  
 * @author faulcon
 *
 */
public class BioSamplesClient implements AutoCloseable {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final SampleRetrievalService sampleRetrievalService;
	private final SamplePageRetrievalService samplePageRetrievalService;
	private final SampleCursorRetrievalService sampleCursorRetrievalService;
	private final SampleSubmissionService sampleSubmissionService;
	private final CurationRetrievalService curationRetrievalService;
	private final CurationSubmissionService curationSubmissionService;
	
	private final SampleValidator sampleValidator;
	
	private final ExecutorService threadPoolExecutor;
	
	BioSamplesClient(URI uri, RestTemplateBuilder restTemplateBuilder,
			SampleValidator sampleValidator, AapClientService aapClientService, 
			BioSamplesProperties bioSamplesProperties) {
		
		threadPoolExecutor = AdaptiveThreadPoolExecutor.create(100, 10000, true, 
				bioSamplesProperties.getBiosamplesClientThreadCount(),
				bioSamplesProperties.getBiosamplesClientThreadCountMax());
		
		RestTemplate restOperations = restTemplateBuilder.build();
				
		if (aapClientService != null) {		
			log.trace("Adding AapClientHttpRequestInterceptor");
			restOperations.getInterceptors().add(new AapClientHttpRequestInterceptor(aapClientService));
		} else {
			log.trace("No AapClientService avaliable");
		}
		
		Traverson traverson = new Traverson(uri, MediaTypes.HAL_JSON);
		traverson.setRestOperations(restOperations);
		
		sampleRetrievalService = new SampleRetrievalService(restOperations, traverson, threadPoolExecutor, bioSamplesProperties.getBiosamplesClientPagesize());
		samplePageRetrievalService = new SamplePageRetrievalService(restOperations, traverson, threadPoolExecutor, bioSamplesProperties.getBiosamplesClientPagesize());
		sampleCursorRetrievalService = new SampleCursorRetrievalService(restOperations, traverson, threadPoolExecutor, bioSamplesProperties.getBiosamplesClientPagesize());
		
		sampleSubmissionService = new SampleSubmissionService(restOperations, traverson, threadPoolExecutor);
		curationRetrievalService = new CurationRetrievalService(restOperations, traverson, threadPoolExecutor, bioSamplesProperties.getBiosamplesClientPagesize());
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
				if (jwt != null) {
					request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer "+jwt);
				}
			}

			//pass along to the next interceptor
			return execution.execute(request, body);
		}
		
	}
    
    @PreDestroy
    public void close() {
		threadPoolExecutor.shutdownNow();
		try {
			threadPoolExecutor.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
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

	public Iterable<Resource<Sample>> fetchSampleResourceAll() throws RestClientException {
		return sampleCursorRetrievalService.fetchAll("", Collections.emptyList());
	}

	public Iterable<Resource<Sample>> fetchSampleResourceAll(String text) throws RestClientException {
		return samplePageRetrievalService.fetchAll(text, Collections.emptyList());
	}

	public Iterable<Resource<Sample>> fetchSampleResourceAll(Collection<Filter> filters) {
		return sampleCursorRetrievalService.fetchAll("", filters);
	}

	public Iterable<Resource<Sample>> fetchSampleResourceAll(String text, Collection<Filter> filters) {
		return samplePageRetrievalService.fetchAll(text, filters);
	}

	public Iterable<Optional<Resource<Sample>>> fetchSampleResourceAll(Iterable<String> accessions) throws RestClientException {
		return sampleRetrievalService.fetchAll(accessions);
	}

	/**
	 * Search for samples using pagination. This method should be used for specific pagination needs. When in need for
	 * all results from a search, prefer the iterator implementation.
	 * @param text
	 * @param page
	 * @param size
	 * @return a paginated results of samples relative to the search term
	 */
	public PagedResources<Resource<Sample>> fetchPagedSampleResource(String text, int page, int size) {
		return samplePageRetrievalService.search(text, Collections.emptyList(), page, size);
	}

	public PagedResources<Resource<Sample>> fetchPagedSampleResource(String text, Collection<Filter> filters, int page, int size) {
		return samplePageRetrievalService.search(text, filters, page, size);
	}

	@Deprecated
	public Optional<Sample> fetchSample(String accession) throws RestClientException {
		Optional<Resource<Sample>> resource = fetchSampleResource(accession);
		if (resource.isPresent()) {
			return Optional.of(resource.get().getContent());
		} else {
			return Optional.empty();
		}
	}	

	@Deprecated
	public Sample persistSample(Sample sample) {
		return persistSampleResource(sample).getContent();
	}
	
	public Resource<Sample> persistSampleResource(Sample sample) {
		return persistSampleResource(sample, null, null);
	}	
	
	public Resource<Sample> persistSampleResource(Sample sample, Boolean setUpdateDate, Boolean setFullDetails)  {
		try {
			return persistSampleResourceAsync(sample, setUpdateDate, setFullDetails).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	public Future<Resource<Sample>> persistSampleResourceAsync(Sample sample) {
		return persistSampleResourceAsync(sample, null, null);
	}	
	
	public Future<Resource<Sample>> persistSampleResourceAsync(Sample sample, Boolean setUpdateDate, Boolean setFullDetails) {
		//validate client-side before submission
		Collection<String> errors = sampleValidator.validate(sample);		
		if (errors.size() > 0) {
			log.error("Errors : "+errors);
			throw new IllegalArgumentException("Sample not valid");
		}
		return sampleSubmissionService.submitAsync(sample, setUpdateDate, setFullDetails);
	}
	
	public Collection<Resource<Sample>> persistSamples(Collection<Sample> samples) {
		return persistSamples(samples, null, null);
	}
	
	public Collection<Resource<Sample>> persistSamples(Collection<Sample> samples, Boolean setUpdateDate, Boolean setFullDetails) {
		List<Resource<Sample>> results = new ArrayList<>();
		List<Future<Resource<Sample>>> futures = new ArrayList<>();
		
		for (Sample sample : samples) {
			futures.add(persistSampleResourceAsync(sample, setUpdateDate, setFullDetails));
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
	
	public Iterable<Resource<Curation>> fetchCurationResourceAll() {
		return curationRetrievalService.fetchAll();
	}

	public Resource<CurationLink> persistCuration(String accession, Curation curation, String domain) {
		return curationSubmissionService.submit(CurationLink.build(accession, curation, domain, null));
	}
}
