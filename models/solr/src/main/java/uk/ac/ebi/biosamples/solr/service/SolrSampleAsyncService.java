package uk.ac.ebi.biosamples.solr.service;

import java.util.concurrent.Future;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import uk.ac.ebi.biosamples.solr.model.SolrSample;

/**
 * This is a wrapper for SolrSampleService but due to Springs
 * implementation of proxys, must be a different class so
 * the proxy can be autowired appropriately
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
}
