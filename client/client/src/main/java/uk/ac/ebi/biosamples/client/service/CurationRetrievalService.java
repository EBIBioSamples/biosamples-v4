package uk.ac.ebi.biosamples.client.service;

import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.client.Hop;
import org.springframework.hateoas.client.Traverson;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;

import uk.ac.ebi.biosamples.client.utils.IterableResourceFetchAll;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;

public class CurationRetrievalService {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final Traverson traverson;
	private final ExecutorService executor;
	private final RestOperations restOperations;
	private final int pageSize;

	public CurationRetrievalService(RestOperations restOperations, Traverson traverson, ExecutorService executor, int pageSize) {
		this.restOperations = restOperations;
		this.traverson = traverson;
		this.executor = executor;
		this.pageSize = pageSize;
	}

	public Iterable<Resource<Curation>> fetchAll() {
		MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
		params.add("size", Integer.toString(pageSize));
		return new IterableResourceFetchAll<Curation>(executor, traverson, restOperations,
				new ParameterizedTypeReference<PagedResources<Resource<Curation>>>() {}, 
				params, "curations");
	}

	public Iterable<Resource<CurationLink>> fetchCurationLinksOfSample(String accession) {
		MultiValueMap<String,String> params = new LinkedMultiValueMap<>();
		params.add("size", Integer.toString(pageSize));
		return new IterableResourceFetchAll<CurationLink>(executor, traverson, restOperations,
				new ParameterizedTypeReference<PagedResources<Resource<CurationLink>>>() {}, 
				params, Hop.rel("samples").withParameter("accession", accession), Hop.rel("curationLinks"));
	}
}
