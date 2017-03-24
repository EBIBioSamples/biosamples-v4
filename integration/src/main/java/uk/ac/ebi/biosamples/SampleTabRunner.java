package uk.ac.ebi.biosamples;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Scanner;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SampleTabRunner implements ApplicationRunner, ExitCodeGenerator, Ordered {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private IntegrationProperties integrationProperties;

	@Autowired
	private RestOperations restTemplate;
	
	private int exitCode = 1;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Starting SampleTabRunner"); 		
		
		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab()).pathSegment("sampletab","v4").build().toUri();

		if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 1) {	
			
			runCallableOnSampleTabResource("/GSB-32.txt", sampleTabString -> {				
				log.info("PUTing to "+uri);
				RequestEntity<String> request = RequestEntity.post(uri).contentType(MediaType.APPLICATION_JSON).body(sampleTabString);
				ResponseEntity<String> response = restTemplate.exchange(request, String.class);				
				//TODO check at the right URLs with GET to make sure all arrived
				//TODO check UTF-8 characters
				});	
			
			runCallableOnSampleTabResource("/GSB-32_unaccession.txt", sampleTabString -> {				
				log.info("PUTing to "+uri);
				RequestEntity<String> request = RequestEntity.post(uri).contentType(MediaType.APPLICATION_JSON).body(sampleTabString);
				ResponseEntity<String> response = restTemplate.exchange(request, String.class);	
				//TODO check at the right URLs with GET to make sure all arrived
				});
			
			runCallableOnSampleTabResource("/GSB-1004.txt", sampleTabString -> {				
				log.info("PUTing to "+uri);
				RequestEntity<String> request = RequestEntity.post(uri).contentType(MediaType.APPLICATION_JSON).body(sampleTabString);
				ResponseEntity<String> response = restTemplate.exchange(request, String.class);	
				//TODO check that SAMEA103886236 does not exist
				});
			
			runCallableOnSampleTabResource("/GSB-1000.txt", sampleTabString -> {				
				log.info("PUTing to "+uri);
				RequestEntity<String> request = RequestEntity.post(uri).contentType(MediaType.APPLICATION_JSON).body(sampleTabString);
				ResponseEntity<String> response = restTemplate.exchange(request, String.class);	
				//TODO check that SAMEA103886236 does exist
				});

		} else if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 2) {
			//TODO check that SAMEA103886236 is a "member of" SAMEG318804
		}
		
		//if we got here without throwing, then we finished sucessfully
		exitCode = 0;
		log.info("Finished SampleTabRunner");
		
	}
	

	private interface SampleTabCallback {
		public void callback(String sampleTabString);
	}
	
	private void runCallableOnSampleTabResource(String resource, SampleTabCallback callback) {

		Scanner scanner = null;
		String sampleTabString = null;	
		
		try {
			scanner = new Scanner(this.getClass().getResourceAsStream(resource), "UTF-8");
			sampleTabString = scanner.useDelimiter("\\A").next();
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
		if (sampleTabString != null) {			
			callback.callback(sampleTabString);
		}
	}

	@Override
	public int getOrder() {
		return 3;
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

}
