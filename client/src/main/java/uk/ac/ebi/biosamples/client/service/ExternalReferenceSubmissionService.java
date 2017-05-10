package uk.ac.ebi.biosamples.client.service;

import java.net.URI;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Hop;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.ExternalReferenceLink;

public class ExternalReferenceSubmissionService {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final Traverson traverson;
	private final ExecutorService executor;
	private final RestOperations restOperations;

	public ExternalReferenceSubmissionService(RestOperations restOperations, Traverson traverson,
			ExecutorService executor) {
		this.restOperations = restOperations;
		this.traverson = traverson;
		this.executor = executor;
	}

	// TODO make async
	public Resource<ExternalReferenceLink> persistExternalReference(String accession,
			ExternalReference externalReference) throws RestClientException {

		URI target = URI.create(traverson.follow("samples")
				.follow(Hop.rel("sample").withParameter("accession", accession))
				.follow("externalReferenceLinks")
				.asLink().getHref());

		log.info("POSTing to " + target + " " + externalReference);

		RequestEntity<ExternalReference> requestEntity = RequestEntity.post(target)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaTypes.HAL_JSON).body(externalReference);
		ResponseEntity<Resource<ExternalReferenceLink>> responseEntity = restOperations.exchange(requestEntity,
				new ParameterizedTypeReference<Resource<ExternalReferenceLink>>() {
				});

		return responseEntity.getBody();
	}
}
