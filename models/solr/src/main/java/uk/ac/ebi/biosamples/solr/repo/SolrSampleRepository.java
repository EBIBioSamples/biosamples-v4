package uk.ac.ebi.biosamples.solr.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.SolrResultPage;
import org.springframework.data.solr.repository.Facet;
import org.springframework.data.solr.repository.Query;
import org.springframework.data.solr.repository.SolrCrudRepository;

import uk.ac.ebi.biosamples.solr.model.SolrSample;

public interface SolrSampleRepository extends SolrCrudRepository<SolrSample, String> {

	@Query(value = "?0")
	FacetPage<SolrSample> findByText(String text, Pageable page);
	@Query(value = "?0")
	@Facet(fields = { "organism_av_ss" }, limit = 10)
	FacetPage<SolrSample> findByTextWithFacets(String text, Pageable page);
	@Query(value = "?0")
	@Facet(fields = { "attributetypes_ss" }, limit = 10)
	FacetPage<SolrSample> findByTextWithFacetTypes(String text, Pageable page);

	@Query(value = "?0 AND release_dt:[* TO NOW]")
	FacetPage<SolrSample> findByTextAndPublic(String text, Pageable page);
	@Query(value = "?0 AND release_dt:[* TO NOW]")
	@Facet(fields = { "?1", "?2", "?3", "?4", "?5", "?6", "?7", "?8", "?9", "?10"}, limit = 10)
	FacetPage<SolrSample> findByTextAndPublicWithFacets(String text, String facet1, String facet2, 
			String facet3, String facet4, String facet5, String facet6, String facet7, String facet8, 
			String facet9, String facet10, Pageable page);
	@Query(value = "?0 AND release_dt:[* TO NOW]")
	@Facet(fields = { "attributetypes_ss" }, limit = 10)
	FacetPage<SolrSample> findByTextAndPublicWithFacetTypes(String text, Pageable page);

	@Query(value = "release_dt:[* TO NOW]")
	FacetPage<SolrSample> findPublic(Pageable page);
	@Query(value = "release_dt:[* TO NOW]")
	@Facet(fields = { "organism_av_ss" }, limit = 10)
	FacetPage<SolrSample> findPublicWithFacets(Pageable page);
	
}
