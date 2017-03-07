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
	SolrResultPage<SolrSample> findByText(String text, Pageable page);

	@Query(value = "?0 AND release_dt:[* TO NOW]")
	SolrResultPage<SolrSample> findByTextAndPublic(String text, Pageable page);

	@Query(value = "?0")
	@Facet(fields = { "organism_av_ss" }, limit = 10)
	SolrResultPage<SolrSample> findByTextWithFacets(String text, Pageable page);

	@Query(value = "?0 AND release_dt:[* TO NOW]")
	@Facet(fields = { "organism_av_ss" }, limit = 10)
	SolrResultPage<SolrSample> findByTextAndPublicWithFacets(String text, Pageable page);

	@Query(value = "release_dt:[* TO NOW]")
	SolrResultPage<SolrSample> findPublic(Pageable page);
}
