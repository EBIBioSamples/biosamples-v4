package uk.ac.ebi.biosamples.zooma;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class ZoomaProcessor {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final RestOperations restOperations;
	
	//TODO make this an application.properties value
	private final UriComponents uriBuilder = 
			UriComponentsBuilder.fromUriString("http://wwwdev.ebi.ac.uk/spot/zooma/v2/api/services/annotate?propertyValue={value}&propertyType={type}").build();
		
	public ZoomaProcessor(RestTemplateBuilder restTemplateBuilder) {
		this.restOperations = restTemplateBuilder.build();
	}
	
	
	@Cacheable("zooma")
	public Optional<String> queryZooma(String type, String value) {
		log.info("Zooma getting : "+type+" : "+value);
		URI uri = uriBuilder.expand(value, type).encode().toUri();
		//log.info("Zooma uri : "+uri);
		
		RequestEntity<Void> requestEntity = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<List<JsonNode>> responseEntity = restOperations.exchange(requestEntity,
				new ParameterizedTypeReference<List<JsonNode>>(){});
		
		//if zero or more than one result found, abort
		if (responseEntity.getBody().size() != 1) {
			return Optional.empty();
		}
		JsonNode n = responseEntity.getBody().get(0);
		
		//if result is anything other than "high" confidence, abort
		if (!n.has("confidence") || !n.get("confidence").asText().equals("HIGH")) {
			return Optional.empty();
		}
		
		//if result has anything other than 1 semantic tag, abort
		if (!n.has("semanticTags") || n.get("semanticTags").size() != 1) {
			return null;
		}
		
		return Optional.of(n.get("semanticTags").get(0).asText());
		
	}
	
	
}
