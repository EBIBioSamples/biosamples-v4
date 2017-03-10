package uk.ac.ebi.biosamples.client.service;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.client.ClientProperties;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class RetrievalService {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private ClientProperties clientProperties;
	
	//use RestOperations as the interface implemented by RestTemplate
	//easier to mock for testing
	@Autowired
	private RestOperations restOperations;

	/**
	 * This will get an existing sample from biosamples using the accession
	 * 
	 * @param sample
	 * @return
	 */
	public Resource<Sample> fetch(String accession) {			
		URI uri = UriComponentsBuilder.fromUri(clientProperties.getBiosampleSubmissionUri())
				.pathSegment("samples",accession)
				.build().toUri();
		
		log.info("GETing "+uri);
		
		RequestEntity<Void> requestEntity = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
		ResponseEntity<Resource<Sample>> responseEntity = restOperations.exchange(requestEntity, new ParameterizedTypeReference<Resource<Sample>>(){});
					
		if (!responseEntity.getStatusCode().is2xxSuccessful()) {
			log.error("Unable to GET "+accession+" : "+responseEntity.toString());
			throw new RuntimeException("Problem GETing "+accession);
		}			
		return responseEntity.getBody();		
	}

}
