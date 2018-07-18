package uk.ac.ebi.biosamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.Scanner;

@Component
public class SampleTabXmlSampleIntegration extends AbstractIntegration {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final IntegrationProperties integrationProperties;

	private final RestOperations restTemplate;

	private final URI uri;
	
	public SampleTabXmlSampleIntegration(RestTemplateBuilder restTemplateBuilder, IntegrationProperties integrationProperties, BioSamplesClient client) {
        super(client);
		this.restTemplate = restTemplateBuilder.build();
		this.integrationProperties = integrationProperties;

		uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUriSampleTab())
			.pathSegment("api", "v2","source","biosamples","sample")
			.queryParam("apikey", integrationProperties.getLegacyApiKey())
			.build().toUri();
	}

	@Override
	protected void phaseOne() {

		runCallableOnResource("/SAMTSTXML1_unaccession.xml", sampleTabString -> {
			log.info("POSTing to " + uri);
			RequestEntity<String> request = RequestEntity.post(uri)
					.contentType(MediaType.APPLICATION_XML)
					.accept(MediaType.TEXT_PLAIN)
					//.header("Accept","text/plain;q=0.9, */*;q=0.1")
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

		URI putUri = UriComponentsBuilder.fromUri(uri).pathSegment("SAME12345679").build().toUri();
		runCallableOnResource("/SAMTSTXML1.xml", sampleTabString -> {
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

		URI putUriNcbi = UriComponentsBuilder.fromUri(uri).pathSegment("SAMD00046940").build().toUri();
		runCallableOnResource("/SAMD00046940.xml", sampleTabString -> {
			log.info("PUTing to " + putUriNcbi);
			RequestEntity<String> request = RequestEntity.put(putUriNcbi)
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

		//Test Database URI is working as expected
		runCallableOnResource("/BSD-957.xml", sampleTabString -> {
			log.info("POSTing to " + uri);
			RequestEntity<String> request = RequestEntity.post(uri)
					.contentType(MediaType.APPLICATION_XML)
					.accept(MediaType.TEXT_PLAIN)
					//.header("Accept","text/plain;q=0.9, */*;q=0.1")
					.body(sampleTabString);
			ResponseEntity<String> response = null;
			try {
				response = restTemplate.exchange(request, String.class);
			} catch (HttpStatusCodeException e) {
				log.info("error response = " + response);
				throw e;
			}
			// TODO check at the right URLs with GET to make sure all
			// arrived
		});

		//test ENA getting accession for null body with local name
		URI uriPostFoosiz =  UriComponentsBuilder.fromUri(uri).pathSegment("foosiz").build().toUri();
		log.info("POSTing to " + putUriNcbi);
		RequestEntity<Void> request = RequestEntity.post(uriPostFoosiz)
				//.contentType(MediaType.APPLICATION_XML)
				.accept(MediaType.TEXT_PLAIN)					
				.build();
		ResponseEntity<String> response = null;
		try {
			response = restTemplate.exchange(request, String.class);
		} catch (HttpStatusCodeException e) {
			log.info("error response = "+response);
			throw e;
		}

	}

	@Override
	protected void phaseTwo() {
		String nameToSearch = "CENSOi007-A";
		Filter nameFilter = FilterBuilder.create().onName(nameToSearch).build();
		Iterator<Resource<Sample>> resourceIterator = client.fetchSampleResourceAll(Collections.singletonList(nameFilter)).iterator();
		if(resourceIterator.hasNext()) {
			Resource<Sample> sample = resourceIterator.next();
			if (! sample.getContent().getExternalReferences().first().getUrl().equals("http://hpscreg.local/cell-line/CENSOi007-A")) {
				throw new RuntimeException(nameToSearch + " XML submitted sample does not contain DatabaseURI as an External Reference");
			}
			if (resourceIterator.hasNext()) {
				throw new RuntimeException("Unexpected number of samples matching name filter '"+ nameToSearch + "'. Should be 1");
			}
		} else {
			throw new RuntimeException("No sample found using filter on name '"+ nameToSearch +"'");
		}


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
