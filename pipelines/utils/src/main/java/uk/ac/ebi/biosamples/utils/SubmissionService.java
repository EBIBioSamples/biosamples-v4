package uk.ac.ebi.biosamples.utils;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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


	public void submit(Sample sample) {
		//if the sample has an accession, put to that
		if (sample.getAccession() != null) {
			//samples with an existing accession should be PUT
			
			URI putUri = UriComponentsBuilder.fromUri(pipelinesProperties.getBiosampleSubmissionURI())
					.path("samples/")
					.path(sample.getAccession())
					.build().toUri();
			
			log.info("PUTing "+putUri);
			
			HttpHeaders headers = new HttpHeaders();
			headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
			RequestEntity<Sample> requestEntity = new RequestEntity<>(sample, headers, HttpMethod.PUT, putUri);
			ResponseEntity<Sample> putResponse = restTemplate.exchange(requestEntity, Sample.class);
			
	//		HttpEntity<Sample> requestEntity = new HttpEntity<>(sample);		
	//		ResponseEntity<Sample> putResponse = restTemplate.exchange(putUri,
	//				HttpMethod.PUT,
	//				requestEntity,
	//				new ParameterizedTypeReference<Sample>(){});
			
			if (!putResponse.getStatusCode().is2xxSuccessful()) {
				log.error("Unable to PUT "+sample.getAccession()+" : "+putResponse.toString());
				throw new RuntimeException("Problem PUTing "+sample.getAccession());
			}
		} else {
			throw new RuntimeException("POST not implemented");
		}
	}
}
