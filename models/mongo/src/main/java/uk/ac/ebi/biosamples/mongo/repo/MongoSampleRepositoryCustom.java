package uk.ac.ebi.biosamples.mongo.repo;

import uk.ac.ebi.biosamples.model.StaticViewWrapper;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;

public interface MongoSampleRepositoryCustom {	
	
	public MongoSample insertNew(MongoSample sample);

	//to provide static view of samples
	public void insertSampleToCollection(MongoSample sample, StaticViewWrapper.StaticView collectionName);
	public MongoSample findSampleFromCollection(String accession, StaticViewWrapper.StaticView collectionName);
}