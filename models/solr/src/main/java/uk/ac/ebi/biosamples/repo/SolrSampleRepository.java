package uk.ac.ebi.biosamples.repo;

import org.springframework.data.solr.repository.SolrCrudRepository;

import uk.ac.ebi.biosamples.models.SolrSample;

public interface SolrSampleRepository extends SolrCrudRepository<SolrSample, String> {

}
