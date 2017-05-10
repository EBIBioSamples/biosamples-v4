package uk.ac.ebi.biosamples.solr.service;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import uk.ac.ebi.biosamples.solr.model.SolrSample;

/**
 * This service provides functionality to ensure that only one thread calls a method at a time.
 * 
 * If other threads call the same method, then they will all wait for the result from the first thread.
 * 
 *  This is particularly useful in case of long-running solr queries; only one thread will call it and
 *  therefore solr will not get into a death spiral trying to service more and more requests.
 * 
 * @author faulcon
 *
 */
@Component
public class SolrSampleThreadSafeService {
	
	private final ConcurrentMap<Marker, Future<Page<SolrSample>>> markerMap = new ConcurrentHashMap<>();
	
	private final SolrSampleAsyncService solrSampleAsyncService;

	private Logger log = LoggerFactory.getLogger(getClass());
	
	public SolrSampleThreadSafeService(SolrSampleAsyncService solrSampleAsyncService) {
		this.solrSampleAsyncService = solrSampleAsyncService;
	}		
	
	public Page<SolrSample> fetchSolrSampleByText(String searchTerm, MultiValueMap<String,String> filters, Pageable pageable) {
		
		Marker marker = new Marker(searchTerm, filters, pageable);
		
		Future<Page<SolrSample>> future = markerMap.computeIfAbsent(marker, 
				m -> solrSampleAsyncService.fetchSolrSampleByText(m.searchTerm, m.filters, m.pageable));
		
		Page<SolrSample> page;
		
		try {
			page = future.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
		
		markerMap.remove(marker, future);
		
		return page;
	}
	
	
	private static class Marker {
		public final String searchTerm;
		public final MultiValueMap<String,String> filters;
		public final Pageable pageable;
	
		public Marker(String searchTerm, MultiValueMap<String,String> filters, Pageable pageable) {
			this.searchTerm = searchTerm;
			this.filters = filters;
			this.pageable = pageable;
		}

		@Override
	    public boolean equals(Object o) {

	        if (o == this) return true;
	        if (!(o instanceof Marker)) {
	            return false;
	        }
	        Marker other = (Marker) o;
	        return Objects.equals(this.searchTerm, other.searchTerm) 
	        		&& Objects.equals(this.filters, other.filters)
	        		&& Objects.equals(this.pageable, other.pageable);
	    }
	    
	    @Override
	    public int hashCode() {
	    	return Objects.hash(searchTerm, filters, pageable);
	    }
	}
	
}
