package uk.ac.ebi.biosamples.client.service;

import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Traverson;
import org.springframework.web.client.RestOperations;

import uk.ac.ebi.biosamples.client.utils.IterableResourceFetchAll;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.Sample;

public class CurationRetrievalService {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final Traverson traverson;
	private final ExecutorService executor;
	private final RestOperations restOperations;

	public CurationRetrievalService(RestOperations restOperations, Traverson traverson, ExecutorService executor) {
		this.restOperations = restOperations;
		this.traverson = traverson;
		this.executor = executor;
	}

	public Iterable<Resource<Curation>> fetchAll() {
		return new IterableResourceFetchAll<Curation>(traverson, restOperations, 
				new ParameterizedTypeReference<PagedResources<Resource<Curation>>>() {}, "curations");
	}
}
