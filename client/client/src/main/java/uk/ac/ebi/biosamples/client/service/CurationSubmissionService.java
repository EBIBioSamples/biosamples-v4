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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import uk.ac.ebi.biosamples.model.CurationLink;

public class CurationSubmissionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CurationSubmissionService.class);

	private final Traverson traverson;
	private final ExecutorService executor;
	private final RestOperations restOperations;

	public CurationSubmissionService(RestOperations restOperations, Traverson traverson,
			ExecutorService executor) {
		this.restOperations = restOperations;
		this.traverson = traverson;
		this.executor = executor;
	}

	public Resource<CurationLink> submit(CurationLink curationLink) throws RestClientException {
		return persistCuration(curationLink, null);
	}

	public Resource<CurationLink> persistCuration(CurationLink curationLink, String jwt) throws RestClientException {
		URI target = URI.create(traverson.follow("samples")
				.follow(Hop.rel("sample").withParameter("accession", curationLink.getSample()))
				.follow("curationLinks")
				.asLink().getHref());

		LOGGER.trace("POSTing to {} {}", target, curationLink);

		RequestEntity<CurationLink> requestEntity = RequestEntity.post(target)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaTypes.HAL_JSON).body(curationLink);
		if (jwt != null) {
			requestEntity.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
		}

		ResponseEntity<Resource<CurationLink>> responseEntity = restOperations.exchange(requestEntity,
				new ParameterizedTypeReference<Resource<CurationLink>>() {
				});

		return responseEntity.getBody();

	}

	public void deleteCurationLink(String sample, String hash) {
		deleteCurationLink(sample, hash, null);
	}

	public void deleteCurationLink(String sample, String hash, String jwt) {

		URI target = URI.create(traverson
				.follow("samples")
				.follow(Hop.rel("sample").withParameter("accession", sample))
				.follow(Hop.rel("curationLink").withParameter("hash", hash))
				.asLink().getHref());
		LOGGER.trace("DELETEing {}", target);

		RequestEntity requestEntity = RequestEntity.delete(target).build();
		if (jwt != null) {
			requestEntity.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
		}
		restOperations.exchange(requestEntity, Void.class);
	}


}
