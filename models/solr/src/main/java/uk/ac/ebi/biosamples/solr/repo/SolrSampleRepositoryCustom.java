package uk.ac.ebi.biosamples.solr.repo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;

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
	public Page<FacetFieldEntry> getFacetFields(String text, Pageable facetPageable);
	

	/**
	 * Return a result of facets over the provided fields with the provided facet paging 
	 * information (offset and count). 
	 * 
	 * @param text
	 * @param facetFields
	 * @param facetPageable
	 * @return
	 */
	public FacetPage<?> getFacets(String text, List<String> facetFields, Pageable facetPageable);
}
