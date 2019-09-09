package uk.ac.ebi.biosamples.client.service;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
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
import org.springframework.hateoas.client.Hop;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.client.Traverson.TraversalBuilder;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.StaticViewWrapper;

public class SampleRetrievalService {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public static final DateTimeFormatter solrFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'");

	private static final ParameterizedTypeReference<PagedResources<Resource<Sample>>> parameterizedTypeReferencePagedResourcesSample = new ParameterizedTypeReference<PagedResources<Resource<Sample>>>(){};
	
	private final Traverson traverson;
	private final ExecutorService executor;
	private final RestOperations restOperations;
	
	public SampleRetrievalService(RestOperations restOperations, Traverson traverson,
			ExecutorService executor) {
		this.restOperations = restOperations;
		this.traverson = traverson;
		this.executor = executor;
	}

	/**
	 * This will get an existing sample from biosamples using the accession
	 * 
	 * @param sample
	 * @return
	 */
	public Future<Optional<Resource<Sample>>> fetch(String accession, Optional<List<String>> curationDomains) {
		return executor.submit(new FetchCallable(accession, curationDomains));
	}

	public Future<Optional<Resource<Sample>>> fetch(String accession, Optional<List<String>> curationDomains, String jwt) {
		return executor.submit(new FetchCallable(accession, curationDomains, jwt));
	}

	public Future<Optional<Resource<Sample>>> fetch(String accession, Optional<List<String>> curationDomains, String jwt, StaticViewWrapper.StaticView staticView) {
		return executor.submit(new FetchCallable(accession, curationDomains, jwt, staticView));
	}

	private class FetchCallable implements Callable<Optional<Resource<Sample>>> {

		private final String accession;
		private final Optional<List<String>> curationDomains;
		private final String jwt;
		private final StaticViewWrapper.StaticView staticView;

		public FetchCallable(String accession, Optional<List<String>> curationDomains) {
			this.accession = accession;
			this.curationDomains = curationDomains;
			this.jwt = null;
			this.staticView = StaticViewWrapper.StaticView.SAMPLES_CURATED;
		}

		public FetchCallable(String accession, Optional<List<String>> curationDomains, String jwt) {
			this.accession = accession;
			this.curationDomains = curationDomains;
			this.jwt = jwt;
			this.staticView = StaticViewWrapper.StaticView.SAMPLES_CURATED;
		}

		public FetchCallable(String accession, Optional<List<String>> curationDomains, String jwt, StaticViewWrapper.StaticView staticView) {
			this.accession = accession;
			this.curationDomains = curationDomains;
			this.jwt = jwt;
			this.staticView = staticView;
		}

		@Override
		public Optional<Resource<Sample>> call() throws Exception {

			URI uri;
			
			if (!curationDomains.isPresent()) {
				uri = URI.create(traverson.follow("samples")
						.follow(Hop.rel("sample")
								.withParameter("accession", accession)
								.withParameter("curationrepo", staticView.getCurationRepositoryName()))
						.asLink().getHref());
			} else {
				TraversalBuilder traversalBuilder = traverson.follow("samples")
						.follow(Hop.rel("sample").withParameter("accession", accession));
				for (String curationDomain : curationDomains.get()) {
					traversalBuilder.follow(Hop.rel("curationDomain")
							.withParameter("curationdomain", curationDomain));
				}
				uri = URI.create(traversalBuilder.asLink().getHref());	
			}

			log.trace("GETing " + uri);

			MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
			headers.add(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON.toString());
			if (jwt != null) {
				headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
			}
			RequestEntity<Void> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, uri);

			ResponseEntity<Resource<Sample>> responseEntity = null;
			try {
				responseEntity = restOperations.exchange(requestEntity,
					new ParameterizedTypeReference<Resource<Sample>>() {
					});
			} catch (HttpStatusCodeException e) {
				if (e.getStatusCode().equals(HttpStatus.FORBIDDEN)
						|| e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
					return Optional.empty();
				} else {
					throw e;
				}
			}
			log.trace("GETted " + uri);

			return Optional.of(responseEntity.getBody());
		}
	}

	public Iterable<Optional<Resource<Sample>>> fetchAll(Iterable<String> accessions) {
		return new IterableResourceFetch(accessions);
	}

	public Iterable<Optional<Resource<Sample>>> fetchAll(Iterable<String> accessions, String jwt) {
		return new IterableResourceFetch(accessions);
	}

	private class IterableResourceFetch implements Iterable<Optional<Resource<Sample>>> {

		private final Iterable<String> accessions;
		private final String jwt;

		public IterableResourceFetch(Iterable<String> accessions) {
			this.accessions = accessions;
			this.jwt = null;
		}

		public IterableResourceFetch(Iterable<String> accessions, String jwt) {
			this.accessions = accessions;
			this.jwt = jwt;
		}

		@Override
		public Iterator<Optional<Resource<Sample>>> iterator() {
			return new IteratorResourceFetch(accessions.iterator());
		}

		private class IteratorResourceFetch implements Iterator<Optional<Resource<Sample>>> {

			private final Iterator<String> accessions;
			private final Queue<Future<Optional<Resource<Sample>>>> queue = new LinkedList<>();
			// TODO application property this
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
			public Optional<Resource<Sample>> next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}

				// fill up the queue if possible
				while (queue.size() < queueMaxSize && accessions.hasNext()) {
					log.trace("Queue size is " + queue.size());
					String nextAccession = accessions.next();
					queue.add(fetch(nextAccession, Optional.empty(), jwt));
				}

				// get the end of the queue and wait for it to finish if needed
				Future<Optional<Resource<Sample>>> future = queue.poll();
				// this shouldn't happen, but best to check
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
