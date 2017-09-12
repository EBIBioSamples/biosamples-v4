package uk.ac.ebi.biosamples.mongo.repo;

import uk.ac.ebi.biosamples.mongo.model.MongoSample;

public interface MongoSampleRepositoryCustom {	
	
	public MongoSample insertNew(MongoSample sample);
}