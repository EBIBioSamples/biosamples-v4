package uk.ac.ebi.biosamples;

import java.net.URI;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;

@Component
@Order(3)
@Profile({"default", "submission"})
public class SampleTabRunner implements ApplicationRunner, ExitCodeGenerator {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private IntegrationProperties integrationProperties;

	@Autowired
	private RestOperations restTemplate;
	
	@Autowired
	private BioSamplesClient biosamplesClient;
	
	private int exitCode = 1;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Starting SampleTabRunner"); 		
		
		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab()).pathSegment("v4").build().toUri();

		if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 1) {	
			
			runCallableOnSampleTabResource("/GSB-32.txt", sampleTabString -> {				
				log.info("PUTing to "+uri);
				RequestEntity<String> request = RequestEntity.post(uri).contentType(MediaType.parseMediaType("text/plain;charset=UTF-8")).body(sampleTabString);
				ResponseEntity<String> response = restTemplate.exchange(request, String.class);				
				//TODO check at the right URLs with GET to make sure all arrived
				//TODO check UTF-8 characters
				if (!biosamplesClient.fetch("SAMEA2186845").getCharacteristics().contains(Attribute.build("description", "Test sample α"))) {
					throw new RuntimeException("SAMEA2186845 does not have 'description':'Test sample α'");
				}
				if (!biosamplesClient.fetch("SAMEA2186844").getCharacteristics().contains(Attribute.build("description", "Test sample β"))) {
					throw new RuntimeException("SAMEA2186844 does not have 'description':'Test sample β'");
				}
				});	
			
			runCallableOnSampleTabResource("/GSB-32_unaccession.txt", sampleTabString -> {				
				log.info("PUTing to "+uri);
				RequestEntity<String> request = RequestEntity.post(uri).contentType(MediaType.TEXT_PLAIN).body(sampleTabString);
				ResponseEntity<String> response = restTemplate.exchange(request, String.class);	
				//TODO check at the right URLs with GET to make sure all arrived
				});
			
			runCallableOnSampleTabResource("/GSB-1004.txt", sampleTabString -> {				
				log.info("PUTing to "+uri);
				RequestEntity<String> request = RequestEntity.post(uri).contentType(MediaType.TEXT_PLAIN).body(sampleTabString);
				ResponseEntity<String> response = restTemplate.exchange(request, String.class);	
				//TODO check that SAMEA103886236 does not exist
				});
			
			runCallableOnSampleTabResource("/GSB-1000.txt", sampleTabString -> {				
				log.info("PUTing to "+uri);
				RequestEntity<String> request = RequestEntity.post(uri).contentType(MediaType.TEXT_PLAIN).body(sampleTabString);
				ResponseEntity<String> response = restTemplate.exchange(request, String.class);	
				//TODO check that SAMEA103886236 does exist
				});

		} else if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 2) {
			//TODO check that SAMEA103886236 is a "member of" SAMEG318804
		}
		
		//if we got here without throwing, then we finished successfully
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
		
		log.trace("sending SampleTab submission \n"+sampleTabString);
		
		if (sampleTabString != null) {			
			callback.callback(sampleTabString);
		}
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

}
