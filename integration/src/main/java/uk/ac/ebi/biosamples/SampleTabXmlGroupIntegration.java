package uk.ac.ebi.biosamples;

import java.net.URI;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.client.BioSamplesClient;

@Component
@Order(6)
@Profile({"default"})
public class SampleTabXmlGroupIntegration extends AbstractIntegration {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final IntegrationProperties integrationProperties;

	private final RestOperations restTemplate;

	private final URI uri;
	
	public SampleTabXmlGroupIntegration(RestTemplateBuilder restTemplateBuilder, IntegrationProperties integrationProperties, BioSamplesClient client) {
        super(client);
		this.restTemplate = restTemplateBuilder.build();
		this.integrationProperties = integrationProperties;

		uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab())
			.pathSegment("api", "v2","source","biosamples","group")
			.queryParam("apikey", integrationProperties.getLegacyApiKey())
			.build().toUri();
	}

	@Override
	protected void phaseOne() {

		runCallableOnResource("/SAMEG123_unaccession.xml", sampleTabString -> {
			log.info("POSTing to " + uri);
			RequestEntity<String> request = RequestEntity.post(uri)
					.contentType(MediaType.APPLICATION_XML)
					.accept(MediaType.TEXT_PLAIN)					
					.body(sampleTabString);
			ResponseEntity<String> response = restTemplate.exchange(request, String.class);
			// TODO check at the right URLs with GET to make sure all
			// arrived
		});

		URI putUri = UriComponentsBuilder.fromUri(uri).pathSegment("SAMEG123").build().toUri();
		runCallableOnResource("/SAMEG123.xml", sampleTabString -> {
			log.info("PUTing to " + putUri);
			RequestEntity<String> request = RequestEntity.put(putUri)
					.contentType(MediaType.APPLICATION_XML)
					.accept(MediaType.TEXT_PLAIN)					
					.body(sampleTabString);
			ResponseEntity<String> response = null;
			try {
				response = restTemplate.exchange(request, String.class);
			} catch (HttpStatusCodeException e) {
				log.info("error response = "+response);
				throw e;
			}
			// TODO check at the right URLs with GET to make sure all
			// arrived
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
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void phaseFive() {
		// TODO Auto-generated method stub
		
	}


	private interface Callback {
		public void callback(String sampleTabString);
	}

	private void runCallableOnResource(String resource, Callback callback) {

		Scanner scanner = null;
		String xmlString = null;

		try {
			scanner = new Scanner(this.getClass().getResourceAsStream(resource), "UTF-8");
			xmlString = scanner.useDelimiter("\\A").next();
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}

		log.trace("sending legacy xml submission \n" + xmlString);

		if (xmlString != null) {
			callback.callback(xmlString);
		}
	}

}
