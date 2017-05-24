package uk.ac.ebi.biosamples;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.SampleFacet;

@Component
@Order(3)
@Profile("rest")
public class RestFacetRunner implements ApplicationRunner, ExitCodeGenerator {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final IntegrationProperties integrationProperties;

	private final RestOperations restTemplate;

	private final BioSamplesClient client;
	
	public RestFacetRunner(RestTemplateBuilder restTemplateBuilder, IntegrationProperties integrationProperties, BioSamplesClient client) {
		this.client = client;
		this.restTemplate = restTemplateBuilder.build();
		this.integrationProperties = integrationProperties;
	}
	
	private int exitCode = 1;

	@Override
	public void run(ApplicationArguments args) throws Exception {

		log.info("Starting RestFacetRunner");
		
		//Sample sampleTest1 = getSampleTest1();

		if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 1) {
		} else if (args.containsOption("phase") && Integer.parseInt(args.getOptionValues("phase").get(0)) == 2) {

			URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri()).pathSegment("samples").pathSegment("facets").build().toUri();

			log.info("GETting from "+uri);
			RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
			ResponseEntity<List<SampleFacet>> response = restTemplate.exchange(request, new ParameterizedTypeReference<List<SampleFacet>>(){});
			//check that there is at least one sample returned
			//if there are zero, then probably nothing was indexed
			if (response.getBody().size() <= 0) {
				throw new RuntimeException("No facets found!");
			}
			if (response.getBody().get(0).size() <= 0) {
				throw new RuntimeException("No facet values found!");
			}
		}
		
		//if we got here without throwing, then we finished sucessfully
		exitCode = 0;
		log.info("Finished RestFacetRunner");
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

}
