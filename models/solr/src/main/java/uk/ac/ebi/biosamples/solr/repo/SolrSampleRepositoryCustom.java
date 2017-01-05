package uk.ac.ebi.biosamples.solr.repo;

import java.io.Serializable;

import uk.ac.ebi.biosamples.solr.model.SolrSample;

public interface SolrSampleRepositoryCustom {

	public SolrSample saveWithin(SolrSample entity, int commitWithinMs);

	public Iterable<SolrSample> saveWithin(Iterable<SolrSample> entities, int commitWithinMs);
	
}
