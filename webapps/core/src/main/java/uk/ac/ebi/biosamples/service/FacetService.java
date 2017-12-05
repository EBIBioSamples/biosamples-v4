package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.solr.service.SolrFacetService;

import java.util.Collection;
import java.util.List;

@Service
public class FacetService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final SolrFacetService solrFacetService;

	public FacetService(SolrFacetService solrSampleService) {
		this.solrFacetService = solrSampleService;
	}


	public List<Facet> getFacets(String text, Collection<Filter> filters, Collection<String> domains, int noOfFacets, int noOfFacetValues) {
		Pageable facetPageable = new PageRequest(0, noOfFacets);
		Pageable facetValuePageable = new PageRequest(0, noOfFacetValues);
		//TODO if a facet is enabled as a filter, then that value will be the only filter displayed
		//TODO allow update date range

		long startTime = System.nanoTime();
		List<Facet> facets = solrFacetService.getFacets(text, filters, domains, facetPageable, facetValuePageable);
		long endTime = System.nanoTime();
		log.trace("Got solr facets in "+((endTime-startTime)/1000000)+"ms");
		
		return facets;
	}

}
