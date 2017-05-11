package uk.ac.ebi.biosamples.solr.service;

import java.util.List;
import java.util.concurrent.Future;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.model.SampleFacet;
import uk.ac.ebi.biosamples.solr.model.SolrSample;

/**
 * This is a wrapper for SolrSampleService but due to Springs
 * implementation of proxys, must be a different class so
 * the proxy can be autowired appropriately.
 * 
 * It is used by SolrSampleThreadSafeService to ensure that
 * only one Future is being prepared at any one time.
 * 
 * @author faulcon
 *
 */
@Service
public class SolrSampleAsyncService {

	private final SolrSampleService solrSampleService;
	
	public SolrSampleAsyncService(SolrSampleService solrSampleService) {
		this.solrSampleService = solrSampleService;
	}
	
	@Async
	public Future<Page<SolrSample>> fetchSolrSampleByText(String searchTerm, MultiValueMap<String,String> filters, Pageable pageable) {
		return new AsyncResult<>(solrSampleService.fetchSolrSampleByText(searchTerm, filters,pageable));
	}
	
	@Async
	public Future<List<SampleFacet>> getFacets(String searchTerm, MultiValueMap<String,String> filters, Pageable facetPageable, Pageable facetValuePageable) {
		return new AsyncResult<>(solrSampleService.getFacets(searchTerm,filters, facetPageable, facetValuePageable));
	}
	
	@Async
	public Future<Autocomplete> getAutocomplete(String autocompletePrefix, MultiValueMap<String,String> filters, int maxSuggestions) {
		return new AsyncResult<>(solrSampleService.getAutocomplete(autocompletePrefix,filters, maxSuggestions));
	}
}
