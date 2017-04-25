package uk.ac.ebi.biosamples.client.service;

import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.client.ClientProperties;
import uk.ac.ebi.biosamples.model.Sample;

public class RetrievalService {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final ClientProperties clientProperties;


	//use RestOperations as the interface implemented by RestTemplate
	//easier to mock for testing
	private final RestOperations restOperations;
	
	private final ExecutorService executor;
	
	public RetrievalService(ClientProperties clientProperties, RestOperations restOperations,
			ExecutorService executor) {
		this.clientProperties = clientProperties;
		this.restOperations = restOperations;
		this.executor = executor;
	}

	/**
	 * This will get an existing sample from biosamples using the accession
	 * 
	 * @param sample
	 * @return
	 */
	public Future<Resource<Sample>> fetch(String accession) {
		return executor.submit(new FetchCallable(accession));
	}

    public PagedResources<Resource<Sample>> fetchPaginated(String text, int startPage, int size) {

		URI uri = UriComponentsBuilder.fromUri(clientProperties.getBiosamplesClientUri())
				.pathSegment("samples")
                .queryParam("text", !text.isEmpty() ? text : "*:*")
                .queryParam("rows", size)
				.queryParam("start", startPage)
				.build().toUri();

		RequestEntity<Void> requestEntity = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
		ResponseEntity<PagedResources<Resource<Sample>>> responseEntity = restOperations.exchange(
				requestEntity,
				new ParameterizedTypeReference<PagedResources<Resource<Sample>>>() { } );

		if (!responseEntity.getStatusCode().is2xxSuccessful()) {
			throw new RuntimeException("Problem GETing samples");
		}

		return responseEntity.getBody();
    }

    private class FetchCallable implements Callable<Resource<Sample>> {

		private final String accession;
		
		public FetchCallable(String accession) {
			this.accession = accession;
		}
		
		@Override
		public Resource<Sample> call() throws Exception {
			
			URI uri = UriComponentsBuilder.fromUri(clientProperties.getBiosamplesClientUri())
					.pathSegment("samples",accession)
					.build().toUri();
			
			log.info("GETing "+uri);
			
			RequestEntity<Void> requestEntity = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
			ResponseEntity<Resource<Sample>> responseEntity = restOperations.exchange(requestEntity, 
		new ParameterizedTypeReference<Resource<Sample>>(){});
						
			if (!responseEntity.getStatusCode().is2xxSuccessful()) {
				log.error("Unable to GET "+accession+" : "+responseEntity.toString());
				throw new RuntimeException("Problem GETing "+accession);
			}			

			log.info("GOTted "+uri);
			
			return responseEntity.getBody();
		}		
	}
	
	public Iterable<Resource<Sample>> fetchAll(Iterable<String> accessions) {
		return new IterableResourceFetch(accessions);
	}
	
	private class IterableResourceFetch implements Iterable<Resource<Sample>> {

		private final Iterable<String> accessions;
		
		public IterableResourceFetch(Iterable<String> accessions) {
			this.accessions = accessions;
		}

		@Override
		public Iterator<Resource<Sample>> iterator() {
			return new IteratorResourceFetch(accessions.iterator());
		}	
	
		private class IteratorResourceFetch implements Iterator<Resource<Sample>> {

			private final Iterator<String> accessions;
			private final Queue<Future<Resource<Sample>>> queue = new LinkedList<>();
			//TODO application property this
			private final int queueMaxSize = 1000;
			
			public IteratorResourceFetch(Iterator<String> accessions) {
				this.accessions = accessions;
			}

			
			@Override
			public boolean hasNext() {
				if (this.accessions.hasNext()) {
					return true;
				} else if (!queue.isEmpty()) {
					return true;
				}
				return false;
			}

			@Override
			public Resource<Sample> next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				
				//fill up the queue if possible
				while (queue.size() < queueMaxSize && accessions.hasNext()) {
					log.info("Queue size is "+queue.size());
					String nextAccession = accessions.next();
					queue.add(fetch(nextAccession));
				}
				
				//get the end of the queue and wait for it to finish if needed
				Future<Resource<Sample>> future = queue.poll();
				//this shouldn't happen, but best to check
				if (future == null) {
					throw new NoSuchElementException();
				}
				
				try {
					return future.get();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
					throw new RuntimeException(e.getCause());
				}
			}
		}
	}
}
