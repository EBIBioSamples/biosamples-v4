package uk.ac.ebi.biosamples.solr.repo;

import org.springframework.data.solr.repository.SolrCrudRepository;

import uk.ac.ebi.biosamples.solr.model.SolrSample;

public interface SolrSampleRepository extends SolrCrudRepository<SolrSample, String> {

}
