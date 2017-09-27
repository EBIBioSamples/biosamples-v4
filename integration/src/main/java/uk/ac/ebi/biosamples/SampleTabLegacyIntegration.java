package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;

@Component
@Order(5)
@Profile({"default"})
public class SampleTabLegacyIntegration extends AbstractIntegration {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final IntegrationProperties integrationProperties;

	private final RestOperations restTemplate;

	private final URI uriSb;
	private final URI uriVa;
	private final URI uriAc;
	
	public SampleTabLegacyIntegration(RestTemplateBuilder restTemplateBuilder, IntegrationProperties integrationProperties, BioSamplesClient client) {
        super(client);
		this.restTemplate = restTemplateBuilder.build();
		this.integrationProperties = integrationProperties;

		uriSb = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab())
			.pathSegment("v1", "json", "sb").build().toUri();
		uriVa = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab())
				.pathSegment("v1", "json", "va").build().toUri();
		uriAc = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab())
				.pathSegment("v1", "json", "ac").build().toUri();
	}

	@Override
	protected void phaseOne() {
		log.info("Testing SampleTab JSON validation");
		runCallableOnSampleTabResource("/GSB-32.json", sampleTabString -> {
			log.info("POSTing to " + uriVa);
			RequestEntity<String> request = RequestEntity.post(uriVa).contentType(MediaType.APPLICATION_JSON)
					.body(sampleTabString);
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			log.info(""+response.getBody());
		});	
		
		log.info("Testing SampleTab JSON accession");
		runCallableOnSampleTabResource("/GSB-32_unaccession.json", sampleTabString -> {
			log.info("POSTing to " + uriAc);
			RequestEntity<String> request = RequestEntity.post(uriVa).contentType(MediaType.APPLICATION_JSON)
					.body(sampleTabString);
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			log.info(""+response.getBody());
		});	
		
		log.info("Testing SampleTab JSON submission");
		runCallableOnSampleTabResource("/GSB-32.json", sampleTabString -> {
			log.info("POSTing to " + uriSb);
			RequestEntity<String> request = RequestEntity.post(uriVa).contentType(MediaType.APPLICATION_JSON)
					.body(sampleTabString);
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			log.info(""+response.getBody());
			//TODO check it has same accessions as accession returned
		});	
	}

	@Override
	protected void phaseTwo() {
		
	}

	@Override
	protected void phaseThree() {
	}

	@Override
	protected void phaseFour() {
		
	}

	@Override
	protected void phaseFive() {
		
	}


	private interface SampleTabCallback {
		public void callback(String sampleTabString);
	}

	private void runCallableOnSampleTabResource(String resource, SampleTabCallback callback) {
		URL url = Resources.getResource(resource);
		String text = null;
		try {
			text = Resources.toString(url, Charsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			callback.callback(text);			
		}
	}

}
