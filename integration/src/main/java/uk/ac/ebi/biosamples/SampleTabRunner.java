package uk.ac.ebi.biosamples;

import java.io.InputStream;
import java.net.URI;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SampleTabRunner implements ApplicationRunner {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private IntegrationProperties integrationProperties;

	@Autowired
	private RestOperations restTemplate;

	@Override
	@Order(2)
	public void run(ApplicationArguments args) throws Exception {
		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionURI()).path("sampletab/").path("v4").build().toUri();
		
		if (args.getOptionNames().contains("phase1")) {			
			
			String sampleTabString = new Scanner(this.getClass().getResourceAsStream("/GSB-32.txt"), "UTF-8").useDelimiter("\\A").next();
			log.info("PUTing to "+uri);
			RequestEntity<String> request = RequestEntity.post(uri).contentType(MediaType.APPLICATION_JSON).body(sampleTabString);
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			
			//TODO check at the right URLs with GET to make sure all arrived
			//TODO check UTF-8 characters
			
	
			sampleTabString = new Scanner(this.getClass().getResourceAsStream("/GSB-32_unaccession.txt"), "UTF-8").useDelimiter("\\A").next();
			request = RequestEntity.post(uri).contentType(MediaType.APPLICATION_JSON).body(sampleTabString);
			response = restTemplate.exchange(request, String.class);
	
			//TODO check at the right URLs with GET to make sure all arrived
		
		}
	}

}
