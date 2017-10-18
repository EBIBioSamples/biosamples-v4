package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.facets.Facet;
import uk.ac.ebi.biosamples.model.facets.FacetType;
import uk.ac.ebi.biosamples.model.filters.Filter;
import uk.ac.ebi.biosamples.model.filters.FilterType;
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

		return solrFacetService.getFacets(text, filters, domains, facetPageable, facetValuePageable);
	}

	/**
	 *	TODO: Duplication of code - we should think of something different
	 *	This could lead to problems where for a FacetType no filter is available
	 *  The code is duplicated over {@see package uk.ac.ebi.biosamples.service.FilterService#from} class
	 */
	public static FilterType from(FacetType type) {
		switch(type) {
			case INCOMING_RELATIONSHIP:
				return FilterType.INVERSE_RELATION_FILTER;
			case OUTGOING_RELATIONSHIP:
				return FilterType.RELATION_FILER;
			case ATTRIBUTE:
				return FilterType.ATTRIBUTE_FILTER;
			case DATE:
				return FilterType.DATE_FILTER;
			default:
				return FilterType.ATTRIBUTE_FILTER;
		}
	}
}
