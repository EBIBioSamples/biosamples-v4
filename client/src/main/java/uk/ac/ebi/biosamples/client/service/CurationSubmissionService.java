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

import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;

public class CurationSubmissionService {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final Traverson traverson;
	private final ExecutorService executor;
	private final RestOperations restOperations;

	public CurationSubmissionService(RestOperations restOperations, Traverson traverson,
			ExecutorService executor) {
		this.restOperations = restOperations;
		this.traverson = traverson;
		this.executor = executor;
	}

	// TODO make async
	public Resource<CurationLink> persistCuration(String accession,
			Curation curation) throws RestClientException {

		URI target = URI.create(traverson.follow("samples")
				.follow(Hop.rel("sample").withParameter("accession", accession))
				.follow("curationLinks")
				.asLink().getHref());

		log.info("POSTing to " + target + " " + curation);

		RequestEntity<Curation> requestEntity = RequestEntity.post(target)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaTypes.HAL_JSON).body(curation);
		ResponseEntity<Resource<CurationLink>> responseEntity = restOperations.exchange(requestEntity,
				new ParameterizedTypeReference<Resource<CurationLink>>() {
				});

		return responseEntity.getBody();
	}
}
