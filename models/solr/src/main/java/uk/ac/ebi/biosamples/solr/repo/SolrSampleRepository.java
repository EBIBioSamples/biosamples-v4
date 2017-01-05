package uk.ac.ebi.biosamples.solr.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.repository.Facet;
import org.springframework.data.solr.repository.Query;
import org.springframework.data.solr.repository.SolrCrudRepository;

import uk.ac.ebi.biosamples.solr.model.SolrSample;

public interface SolrSampleRepository extends SolrCrudRepository<SolrSample, String>, SolrSampleRepositoryCustom {

	@Query(value = "?0")
	FacetPage<SolrSample> findByText(String text, Pageable page);

	@Query(value = "?0")
	@Facet(fields = { "organism_av_ss" }, limit = 10)
	FacetPage<SolrSample> findByTextWithFacets(String text, Pageable page);
}
