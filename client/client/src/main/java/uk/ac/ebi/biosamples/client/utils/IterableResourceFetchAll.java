package uk.ac.ebi.biosamples.client.utils;

import java.net.URI;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.model.Sample;

public class IterableResourceFetchAll<T> implements Iterable<Resource<T>> {
	
	private Logger log = LoggerFactory.getLogger(getClass());	
	
	private final Traverson traverson;
	private final RestOperations restOperations;
	private final String[] rels;
	private final ParameterizedTypeReference<PagedResources<Resource<T>>> parameterizedTypeReference;
	private final MultiValueMap<String,String> params;

	/**
	 * ParameterizedTypeReference must be ParameterizedTypeReference<PagedResources<Resource<T>>> but this
	 * information is lost due to type erasure of the generic on compilation, and therefore has to be
	 * passed manually.
	 * 
	 * @param traverson
	 * @param restOperations
	 * @param parameterizedTypeReference
	 * @param rels
	 */
	public IterableResourceFetchAll(Traverson traverson, RestOperations restOperations, 
			ParameterizedTypeReference<PagedResources<Resource<T>>> parameterizedTypeReference,
			MultiValueMap<String,String> params, String... rels) {
		this.traverson = traverson;
		this.restOperations = restOperations;
		this.rels = rels;
		this.parameterizedTypeReference = parameterizedTypeReference;
		this.params = params;
	}
	
	public Iterator<Resource<T>> iterator() {			
		//get the first page
		//TODO allow sample page size to be customised in property
		URI uri = UriComponentsBuilder.fromHttpUrl(traverson.follow(rels).asLink().getHref())
				.queryParams(params).queryParam("size", "1000").build().toUri();
		RequestEntity<Void> requestEntity = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<PagedResources<Resource<T>>> responseEntity = restOperations.exchange(requestEntity,
				parameterizedTypeReference);
		return new IteratorResourceFetchAll<T>(responseEntity.getBody(), restOperations, parameterizedTypeReference);
	}

	private class IteratorResourceFetchAll<T> implements Iterator<Resource<T>> {
		
		private PagedResources<Resource<T>> page;
		private Iterator<Resource<T>> pageIterator;
		private final RestOperations restOperations;
		private final ParameterizedTypeReference<PagedResources<Resource<T>>> parameterizedTypeReference;
		
		public IteratorResourceFetchAll(PagedResources<Resource<T>> page, RestOperations restOperations,
				ParameterizedTypeReference<PagedResources<Resource<T>>> parameterizedTypeReference) {
			this.page = page;
			this.pageIterator = page.iterator();
			this.restOperations = restOperations;
			this.parameterizedTypeReference = parameterizedTypeReference;
		}
		
		@Override
		public boolean hasNext() {
			//TODO pre-emtively grab the next page as a future
			
			if (pageIterator.hasNext()) {
				return true;
			}
			//does the page have a next page?
			if (page.hasLink(Link.REL_NEXT)) {
				return true;
			}
			return false;
		}
	
		@Override
		public Resource<T> next() {
			Resource<T> next = null;
			while (next == null) {
				if (pageIterator.hasNext()) {
					next = pageIterator.next(); 
				} else if (page.hasLink(Link.REL_NEXT)) {
					//does the page have a next page?
					URI uri = URI.create(page.getLink(Link.REL_NEXT).getHref());
					log.trace("IteratorResourceFetchAll following "+uri.toString());
					RequestEntity<Void> requestEntity = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
					ResponseEntity<PagedResources<Resource<T>>> responseEntity = restOperations.exchange(requestEntity,
							parameterizedTypeReference);
					this.page = responseEntity.getBody();
					this.pageIterator = page.iterator();
					next = this.pageIterator.next();
				} else {
					//no more in this iterator and no more pages, so end	
					throw new NoSuchElementException();
				}
			}
			return next;
		}
			
			
	}
}