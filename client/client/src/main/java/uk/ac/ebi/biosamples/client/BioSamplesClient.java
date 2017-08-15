package uk.ac.ebi.biosamples.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Traverson;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.client.service.CurationRetrievalService;
import uk.ac.ebi.biosamples.client.service.CurationSubmissionService;
import uk.ac.ebi.biosamples.client.service.SampleRetrievalService;
import uk.ac.ebi.biosamples.client.service.SampleSubmissionService;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleValidator;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.StreamSupport;

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
	private final CurationRetrievalService curationRetrievalService;
	private final CurationSubmissionService curationSubmissionService;
	
	private final SampleValidator sampleValidator;
	
	private final ExecutorService threadPoolExecutor;
	
	public BioSamplesClient(ClientProperties clientProperties, RestTemplateBuilder restTemplateBuilder, SampleValidator sampleValidator) {
		//TODO application.properties this
		threadPoolExecutor = Executors.newFixedThreadPool(64);
		
		RestTemplate restOperations = restTemplateBuilder.build();
		
		Traverson traverson = new Traverson(clientProperties.getBiosamplesClientUri(), MediaTypes.HAL_JSON);
		traverson.setRestOperations(restOperations);
		
		sampleRetrievalService = new SampleRetrievalService(restOperations, traverson, threadPoolExecutor, clientProperties);
		sampleSubmissionService = new SampleSubmissionService(restOperations, traverson, threadPoolExecutor);
		curationRetrievalService = new CurationRetrievalService(restOperations, traverson, threadPoolExecutor, clientProperties);
		curationSubmissionService = new CurationSubmissionService(restOperations, traverson, threadPoolExecutor);
		
		this.sampleValidator = sampleValidator;
	}

    @PreDestroy
    public void close() {
    	log.info("Closing thread pool");
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
		return sampleRetrievalService.fetchAll();
	}

	public Iterable<Resource<Sample>> fetchSampleResourceAll(String text) throws RestClientException {
		return sampleRetrievalService.fetchAll(text);
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
	public PagedResources<Resource<Sample>> fetchPagedSamples(String text, int page, int size) {
		return sampleRetrievalService.search(text, page, size);
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
		//validate client-side before submission
		Collection<String> errors = sampleValidator.validate(sample);		
		if (errors.size() > 0) {
			log.info("Errors : "+errors);
			throw new IllegalArgumentException("Sample not valid");
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

	public Iterable<Resource<Curation>> fetchCurationResourceAll() throws RestClientException {
		return curationRetrievalService.fetchAll();
	}
	public Resource<CurationLink> persistCuration(String accession, Curation curation) throws RestClientException {
		return curationSubmissionService.persistCuration(accession, curation);
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

}
