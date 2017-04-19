package uk.ac.ebi.biosamples.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import uk.ac.ebi.biosamples.model.SampleFacet;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;

@Service
public class FacetService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private SolrSampleService solrSampleService;
	
	public List<SampleFacet> getFacets(String text, MultiValueMap<String,String> filters, int noOfFacets, int noOfFacetValues) {
		Pageable facetPageable = new PageRequest(0,noOfFacets);
		Pageable facetValuePageable = new PageRequest(0,noOfFacetValues);
		//TODO if a facet is enabled as a filter, then that value will be the only filter displayed
		return solrSampleService.getFacets(text, filters, facetPageable, facetValuePageable);
	}
}
