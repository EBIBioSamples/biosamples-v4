package uk.ac.ebi.biosamples.ols;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.databind.JsonNode;

import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.utils.ClientUtils;

@Service
public class OlsProcessor {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final RestOperations restOperations;
		
	private final BioSamplesProperties bioSamplesProperties;
		
	public OlsProcessor(RestTemplateBuilder restTemplateBuilder, BioSamplesProperties bioSamplesProperties) {
		this.restOperations = restTemplateBuilder.build();
		this.bioSamplesProperties = bioSamplesProperties;
	}
	
	/**
	 * 
	 * 
	 * @param ontology must be unencoded
	 * @param iri must be unencoded
	 * @return
	 */
	@Cacheable("ols_ancestors_synonyms")
	public Collection<String> ancestorsAndSynonyms(String ontology, String iri) {
		Set<String> synonyms = new HashSet<>();
		if (ontology == null || ontology.trim().length() == 0) { 
			return synonyms;
		}
		if (iri == null || iri.trim().length() == 0) {
			return synonyms;
		}
		
		//check if the iri is a full iri with all the necessary parts
		//build has to flag this iri as having already been encoded
		UriComponents iriComponents = UriComponentsBuilder.fromUriString(iri).build();
		if (iriComponents.getScheme() == null
				|| iriComponents.getHost() == null 
				|| iriComponents.getPath() == null) {
			//incomplete iri (e.g. 9606, EFO_12345) don't bother to check
			return synonyms;
		}

		//TODO do more by hal links, needs OLS to support
		//build has to flag this iri as having already been encoded
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(
				bioSamplesProperties.getOls()+"/api/ontologies/{ontology}/terms/{term}/hierarchicalAncestors?size=1000").build();

		log.trace("Base uriComponents = "+uriComponents);
		
		//have to *double* encode the things we are going to put in the URI
		//uriComponents will encode it once, so we only need to encode it one more time manually
		try {
			ontology = UriUtils.encodePathSegment(ontology, "UTF-8");
			iri = UriUtils.encodePathSegment(iri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			//should never happen
			throw new RuntimeException(e);
		}
		//expand the template using the variables
		URI uri = uriComponents.expand(ontology, iri).toUri();
		
		log.debug("Contacting "+uri);

		//Note: OLS won't accept hal+json on that endpoint
		RequestEntity<Void> requestEntity = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
		ResponseEntity<JsonNode> responseEntity = null;
		try {
			responseEntity = ClientUtils.<Void,JsonNode>doRetryQuery(requestEntity, restOperations, 5, 
					new ParameterizedTypeReference<JsonNode>(){});
		} catch (HttpStatusCodeException e) {
			//if we get a 404, return an empty list
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				return Collections.emptyList();
			}
		}
		
		JsonNode n = responseEntity.getBody();
		if (n.has("_embedded")) {
			if (n.get("_embedded").has("terms")) {
				for (JsonNode o : n.get("_embedded").get("terms")) {
					if (o.has("label")) {
						String synonym = o.get("label").asText();
						if (synonym != null && synonym.trim().length() > 0) {
							log.trace("adding synonym "+synonym);
							synonyms.add(synonym);
						}
					}
					if (o.has("synonyms")) {
						for (JsonNode p : o.get("synonyms")) {
							String synonym = p.asText();
							if (synonym != null && synonym.trim().length() > 0) {
								log.trace("adding synonym "+synonym);
								synonyms.add(synonym);
							}
						}
					}
				}
			}
		}
	
		
		return synonyms;
	}
	
	
	@Cacheable("ols_short")
	public Optional<String> queryOlsForShortcode(String shortcode) {
		log.trace("OLS getting : "+shortcode);

		//TODO do more by hal links, needs OLS to support
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(bioSamplesProperties.getOls()+"/api/terms?id={shortcode}").build();
		URI uri = uriComponents.expand(shortcode).encode().toUri();
		
		RequestEntity<Void> requestEntity = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
		ResponseEntity<JsonNode> responseEntity = restOperations.exchange(requestEntity,
				new ParameterizedTypeReference<JsonNode>(){});
		
		//if zero or more than one result found, abort
		if (responseEntity.getBody().size() != 1) {
			return Optional.empty();
		}
		JsonNode n = responseEntity.getBody();
		
		if (n.has("_embedded")) {
			if (n.get("_embedded").has("terms")) {
				if (n.get("_embedded").get("terms").size() == 1) {
					if (n.get("_embedded").get("terms").get(0).has("iri")) {
						String iri = n.get("_embedded").get("terms").get(0).get("iri").asText();
						log.info("OLS mapped "+shortcode+"  to "+iri);
						return Optional.of(iri);
					}
				}
			}	
		}
		return Optional.empty();
	}
}
