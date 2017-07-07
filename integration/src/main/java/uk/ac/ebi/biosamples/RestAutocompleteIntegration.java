package uk.ac.ebi.biosamples;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Autocomplete;

@Component
@Order(4)
public class RestAutocompleteIntegration extends AbstractIntegration {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final IntegrationProperties integrationProperties;

	private final RestOperations restTemplate;
	
	public RestAutocompleteIntegration(RestTemplateBuilder restTemplateBuilder, IntegrationProperties integrationProperties, BioSamplesClient client) {
		super(client);
		this.restTemplate = restTemplateBuilder.build();
		this.integrationProperties = integrationProperties;
	}
	
	@Override
	protected void phaseOne() {
	}

	@Override
	protected void phaseTwo() {

		URI uri = UriComponentsBuilder.fromUri(integrationProperties.getBiosampleSubmissionUri())
				.pathSegment("samples").pathSegment("autocomplete").build().toUri();

		log.info("GETting from "+uri);
		RequestEntity<Void> request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
		ResponseEntity<Autocomplete> response = restTemplate.exchange(request, new ParameterizedTypeReference<Autocomplete>(){});
		//check that there is at least one sample returned
		//if there are zero, then probably nothing was indexed
		if (response.getBody().getSuggestions().size() <= 0) {
			throw new RuntimeException("No autocomplete suggestions found!");
		}
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

}
