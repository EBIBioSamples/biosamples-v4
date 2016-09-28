package uk.ac.ebi.biosamples.utils;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.UriTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;


@Service
public class HateoasUtils {

	private Logger log = LoggerFactory.getLogger(getClass());


	
	//use RestOperations as the interface implemented by RestTemplate
	//easier to mock for testing
	private final RestOperations restTemplate;
	
	public HateoasUtils(@Autowired RestOperations restTemplate) {
		this.restTemplate = restTemplate;
	}
	
	public <T> Resource<T> getHateoasResource(URI uri, ParameterizedTypeReference<Resource<T>> parameterizedTypeReferece, String... rels) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("application/hal+json"));
		HttpEntity<T> httpEntity = new HttpEntity<>(null, headers);		
		
		return getHateoasResource(uri, parameterizedTypeReferece, httpEntity, rels);
	}
	
	public <T> Resource<T> getHateoasResource(URI uri, ParameterizedTypeReference<Resource<T>> parameterizedTypeReferece, HttpEntity<?> requestEntity, String... rels) {
		return getHateoasResponse(uri, parameterizedTypeReferece, requestEntity, rels).getBody();
	}
	
	public <T extends ResourceSupport> ResponseEntity<T> getHateoasResponse(URI uri, ParameterizedTypeReference<T> parameterizedTypeReferece, String... rels) {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("application/hal+json"));
		HttpEntity<?> httpEntity = new HttpEntity<>(null, headers);	

		return getHateoasResponse(uri, parameterizedTypeReferece, httpEntity, rels);
	}

	
	/**
	 *  
	 * parameterizedTypeReferece should be created as an instance of an anonymous subclass of ParameterizedTypeReference 
	 * e.g. new ParameterizedTypeReference<Reousrce<Thing>>(){}
	 * 
	 * @param uri Starting point of the API
	 * @param parameterizedTypeReferece Needed for generic class preservation
	 * @param requestEntity Optional for control over HTTP requests
	 * @param rels Zero or more relations to follow
	 * @return
	 */
	public <T extends ResourceSupport> ResponseEntity<T> getHateoasResponse(URI uri, ParameterizedTypeReference<T> parameterizedTypeReferece, HttpEntity<?> requestEntity, String... rels) {
		ResponseEntity<T> response = null;
		int i = 0;
		do {
			log.info("Getting URI "+uri);
			
			response = restTemplate.exchange(uri,
					HttpMethod.GET,
					requestEntity,
					parameterizedTypeReferece);
			
			T body = response.getBody();
			if (i < rels.length) {
				//if there is another relation to follow, update the uri and follow it
				Link link = body.getLink(rels[i]);
				UriTemplate uriTemplate = new UriTemplate(link.getHref());
				uri = uriTemplate.expand();
				i++;
			} else {
				//if there is no more relations, return what we've got
				return response;
			}
			//keep doing this as long as there are more relations to follow
		} while (i <= rels.length);
		
		//we should never get here...
		throw new RuntimeException("Unreachable code reached");
	}

	
	public UriTemplate getHateoasUriTemplate(URI uri, String... rels) {

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("application/hal+json"));
		HttpEntity<?> httpEntity = new HttpEntity<>(null, headers);
		return getHateoasUriTemplate(uri, httpEntity, rels);
	}
	
	public UriTemplate getHateoasUriTemplate(URI uri, HttpEntity<?> requestEntity, String... rels) {
		ResponseEntity<Resource<?>> response = null;
		UriTemplate uriTemplate = null;
		int i = 0;
		do {
			log.trace("Getting URI "+uri);
			
			response = restTemplate.exchange(uri,
					HttpMethod.GET,
					requestEntity,
					new ParameterizedTypeReference<Resource<?>>(){});
			
			if (response.getStatusCode() != HttpStatus.OK) {
				throw new RuntimeException("Non-200 reponse for "+uri+" ("+response.getStatusCode()+")");
			}
			
			Resource<?> body = response.getBody();
			if (rels != null && i < rels.length) {
				//if there is another relation to follow, update the uri and follow it
				Link link = body.getLink(rels[i]);
				if (link == null) throw new IllegalArgumentException("Unable to follow relation "+rels[i]+" from "+uri);
				uriTemplate = new UriTemplate(link.getHref());
				uri = uriTemplate.expand();
				i++;
			} else {
				//if there is no more relations, return what we've got
				return uriTemplate;
			}
			//keep doing this as long as there are more relations to follow
		} while (i <= rels.length);
		
		//we should never get here...
		throw new RuntimeException("Unreachable code reached");
	}
	
}
