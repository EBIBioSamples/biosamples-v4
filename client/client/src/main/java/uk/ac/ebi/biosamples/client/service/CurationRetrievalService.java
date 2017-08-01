package uk.ac.ebi.biosamples.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Traverson;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;

import uk.ac.ebi.biosamples.client.ClientProperties;
import uk.ac.ebi.biosamples.client.utils.IterableResourceFetchAll;
import uk.ac.ebi.biosamples.model.Curation;

import java.util.concurrent.ExecutorService;

public class CurationRetrievalService {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final Traverson traverson;
	private final ExecutorService executor;
	private final RestOperations restOperations;
	private final ClientProperties clientProperties;

	public CurationRetrievalService(RestOperations restOperations, Traverson traverson, ExecutorService executor, ClientProperties clientProperties) {
		this.restOperations = restOperations;
		this.traverson = traverson;
		this.executor = executor;
		this.clientProperties = clientProperties;
	}

	public Iterable<Resource<Curation>> fetchAll() {
		MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
		params.add("size", Integer.toString(clientProperties.getBiosamplesClientPagesize()));
		return new IterableResourceFetchAll<Curation>(executor, traverson, restOperations,
				new ParameterizedTypeReference<PagedResources<Resource<Curation>>>() {}, 
				params, "curations");
	}
}
