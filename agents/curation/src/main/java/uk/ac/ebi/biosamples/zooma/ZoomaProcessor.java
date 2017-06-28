package uk.ac.ebi.biosamples.zooma;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class ZoomaProcessor {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final RestOperations restOperations;
	private final BioSamplesClient biosamplesClient;
	
	//TODO make this an application.properties value
	private final UriComponents uriBuilder = 
			UriComponentsBuilder.fromUriString("http://www.ebi.ac.uk/spot/zooma/v2/api/services/annotate?propertyValue={value}&propertyType={type}").build();
	

	private final LoadingCache<List<String>, Optional<String>> zoomaCache = Caffeine.newBuilder()
		    .maximumSize(10000)
		    .build(key -> queryZooma(key.get(0),key.get(1)));
	
	public ZoomaProcessor(RestTemplateBuilder restTemplateBuilder, BioSamplesClient biosamplesClient) {
		this.restOperations = restTemplateBuilder.build();
		this.biosamplesClient = biosamplesClient;
	}
	
	
	public void process(Sample sample) {
		
		for (Attribute attribute : sample.getAttributes()) {
			
			if (attribute.getIri() != null) {
				continue;
			} 
			if (attribute.getUnit() != null) {
				continue;
			} 
			
			if (attribute.getType().equals("synonym")) {
				log.info("Skipping synonym "+attribute.getValue());
				continue;
			} 
			if (attribute.getType().equals("label")) {
				log.info("Skipping label "+attribute.getValue());
				continue;
			}
			if (attribute.getType().equals("host_subject_id")) {
				log.info("Skipping host_subject_id "+attribute.getValue());
				continue;
			}

			
			if (attribute.getValue().matches("^[0-9.-]+$")) {
				log.info("Skipping number "+attribute.getValue());
				continue;
			}
			if (attribute.getValue().matches("^[ESD]R[SRX][0-9]+$")) {
				log.info("Skipping SRA/ENA/DDBJ identifier "+attribute.getValue());
				continue;
			} 
			if (attribute.getValue().matches("^GSM[0-9]+$")) {
				log.info("Skipping GEO identifier "+attribute.getValue());
				continue;
			} 
			if (attribute.getValue().matches("^SAM[END]A?[0-9]+$")) {
				log.info("Skipping BioSample identifier "+attribute.getValue());
				continue;
			}
			
			
			if (attribute.getIri() == null && attribute.getType().length() < 64 && attribute.getValue().length() < 128) {
				Optional<String> iri = cachedQueryZooma(attribute.getType(), attribute.getValue());
				if (iri.isPresent()) {
					log.info("Mapped "+attribute+" to "+iri.get());
					Attribute mapped = Attribute.build(attribute.getType(), attribute.getValue(), iri.get(), null);
					Curation curation = Curation.build(Collections.singleton(attribute), Collections.singleton(mapped), null, null);
				
					//save the curation back in biosamples
					//biosamplesClient.persistCuration(sample.getAccession(), curation);
				}
			}
		}
	}
	
	private Optional<String> cachedQueryZooma(String type, String value) {
		return zoomaCache.get(Arrays.asList(type,value));
	}
	
	private Optional<String> queryZooma(String type, String value) {
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
