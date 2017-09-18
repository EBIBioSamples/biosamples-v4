package uk.ac.ebi.biosamples.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriUtils;
import uk.ac.ebi.biosamples.model.facets.FacetFactory;
import uk.ac.ebi.biosamples.model.facets.LabelCountEntry;
import uk.ac.ebi.biosamples.model.facets.StringListFacet;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FacetService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final SolrSampleService solrSampleService;

	public FacetService(SolrSampleService solrSampleService) {
		this.solrSampleService = solrSampleService;
	}


	public List<Resource<StringListFacet>> getFacets(String text, MultiValueMap<String,String> filters, int noOfFacets, int noOfFacetValues) {
		Pageable facetPageable = new PageRequest(0,noOfFacets);
		Pageable facetValuePageable = new PageRequest(0,noOfFacetValues);
		//TODO if a facet is enabled as a filter, then that value will be the only filter displayed
		//TODO allow update date range
		List<StringListFacet> solrFacets = solrSampleService.getFacets(text, filters, null, null, facetPageable, facetValuePageable);


		List<String> tempFilters = new ArrayList<>();
		for(String key: filters.keySet()) {
			filters.get(key).forEach(value -> tempFilters.add(key + ":" + value));
		}
		String[] filtersArray = tempFilters.toArray(new String[tempFilters.size()]);


		// Need to return a resource for each facet created from solr
		List<Resource<StringListFacet>> resourceFacets = new ArrayList<>();
		for(StringListFacet facet: solrFacets) {
			FacetResourceAssembler.LabelFilterResourceAssembler resourceAssembler =
					new FacetResourceAssembler.LabelFilterResourceAssembler(text, null, null, filtersArray, facet);
			FacetResourceAssembler.StringListFacetResourceAssembler facetResourceAssembler =
					new FacetResourceAssembler.StringListFacetResourceAssembler(text, null, null, filtersArray, facet);
			List<LabelCountEntry> content = (List<LabelCountEntry>) facet.getContent();
			List<Resource<LabelCountEntry>> resourceContent =
					content.stream().map(resourceAssembler::toResource).collect(Collectors.toList());
			StringListFacet newFacet = FacetFactory.buildStringList(facet.getType(), facet.getLabel(), facet.getCount(), resourceContent);
			resourceFacets.add(facetResourceAssembler.toResource(newFacet));
		}
		return resourceFacets;
	}

	private String encodeParam(String queryParam) {
		try {
			return UriUtils.encodeQueryParam(queryParam, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
