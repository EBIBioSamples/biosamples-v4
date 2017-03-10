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
	@Facet(fields = { "attributetypes_ss" }, limit = 10)
	FacetPage<SolrSample> findByTextWithFacetTypes(String text, Pageable page);

	@Query(value = "?0 AND release_dt:[* TO NOW]")
	FacetPage<SolrSample> findByTextAndPublic(String text, Pageable page);
	@Query(value = "?0 AND release_dt:[* TO NOW]")
	@Facet(fields = { "attributetypes_ss" }, limit = 10)
	FacetPage<SolrSample> findByTextAndPublicWithFacetTypes(String text, Pageable page);

	@Query(value = "release_dt:[* TO NOW]")
	FacetPage<SolrSample> findPublic(Pageable page);
	@Query(value = "release_dt:[* TO NOW]")
	@Facet(fields = { "organism_av_ss" }, limit = 10)
	FacetPage<SolrSample> findPublicWithFacets(Pageable page);
	
}
