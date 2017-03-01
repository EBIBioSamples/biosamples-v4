package uk.ac.ebi.biosamples.service;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class SubmissionService {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private PipelinesProperties pipelinesProperties;
	
	//use RestOperations as the interface implemented by RestTemplate
	//easier to mock for testing
	@Autowired
	private RestOperations restTemplate;


	public Resource<Sample> submit(Sample sample) {
		//if the sample has an accession, put to that
		if (sample.getAccession() != null) {
			//samples with an existing accession should be PUT			
			URI uri = UriComponentsBuilder.fromUri(pipelinesProperties.getBiosampleSubmissionURI())
					.path("samples/")
					.path(sample.getAccession())
					.build().toUri();
			
			log.trace("PUTing "+uri);
			
			RequestEntity<Sample> requestEntity = RequestEntity.put(uri).contentType(MediaType.APPLICATION_JSON).body(sample);
			ResponseEntity<Resource<Sample>> responseEntity = restTemplate.exchange(requestEntity, new ParameterizedTypeReference<Resource<Sample>>(){});
						
			if (!responseEntity.getStatusCode().is2xxSuccessful()) {
				log.error("Unable to PUT "+sample.getAccession()+" : "+responseEntity.toString());
				throw new RuntimeException("Problem PUTing "+sample.getAccession());
			}			
			return responseEntity.getBody();
			
		} else {
			//samples without an existing accession should be POST			
			URI uri = UriComponentsBuilder.fromUri(pipelinesProperties.getBiosampleSubmissionURI())
					.path("samples")
					.build().toUri();
			
			log.trace("POSTing "+uri);
			
			RequestEntity<Sample> requestEntity = RequestEntity.post(uri).contentType(MediaType.APPLICATION_JSON).body(sample);
			ResponseEntity<Resource<Sample>> responseEntity = restTemplate.exchange(requestEntity, new ParameterizedTypeReference<Resource<Sample>>(){});
						
			if (!responseEntity.getStatusCode().is2xxSuccessful()) {
				log.error("Unable to POST "+sample.getAccession()+" : "+responseEntity.toString());
				throw new RuntimeException("Problem POSTing "+sample.getAccession());
			}			
			return responseEntity.getBody();
		}
	}
}
