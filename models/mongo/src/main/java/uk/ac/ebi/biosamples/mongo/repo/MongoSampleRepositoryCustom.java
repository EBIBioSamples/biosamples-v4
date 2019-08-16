package uk.ac.ebi.biosamples.mongo.repo;

import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoSampleStaticViews;

public interface MongoSampleRepositoryCustom {	
	
	public MongoSample insertNew(MongoSample sample);

	//to provide static view of samples
	public void insertSample(MongoSample sample, MongoSampleStaticViews collectionName);
	public MongoSample readSample(String accession, MongoSampleStaticViews collectionName);
}