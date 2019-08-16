package uk.ac.ebi.biosamples.mongo.repo;

import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoSampleStaticViews;

public interface MongoSampleRepositoryCustom {	
	
	public MongoSample insertNew(MongoSample sample);

	//to provide static view of samples
	public void insertSampleToCollection(MongoSample sample, MongoSampleStaticViews collectionName);
	public MongoSample findSampleFromCollection(String accession, MongoSampleStaticViews collectionName);
}