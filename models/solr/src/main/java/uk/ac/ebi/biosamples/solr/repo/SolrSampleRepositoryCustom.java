package uk.ac.ebi.biosamples.solr.repo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;

import uk.ac.ebi.biosamples.solr.model.SolrSample;

public interface SolrSampleRepositoryCustom {
	
	/**
	 * Get the attribute types (or other facet fields) for a particular query
	 * and using the pageable to determine the number of offset of *the facets* returned
	 * as a page of facet fields 
	 * 
	 * @param test
	 * @param facetPageable
	 * @return
	 */
	public Page<FacetFieldEntry> getFacetFields(FacetQuery query, Pageable facetPageable);
	

	/**
	 * Return a result of facets over the provided fields with the provided facet paging 
	 * information (offset and count). 
	 * 
	 * @param query
	 * @param facetFields
	 * @param facetPageable
	 * @return
	 */
	public FacetPage<?> getFacets(FacetQuery query, List<String> facetFields, Pageable facetPageable);
	

	/**
	 * Use a query object to get a page of results. This allows for more complicated query
	 * construction compared to a simple string e.g. filtering
	 * 
	 * @param query
	 * @return
	 */
	public Page<SolrSample> findByQuery(Query query);

	/**
	 * Use a query object to get a page of results. This allows for more complicated query
	 * construction compared to a simple string e.g. filtering
	 * 
	 * @param query
	 * @return
	 */
	public FacetPage<SolrSample> findByFacetQuery(FacetQuery query);
	
}
