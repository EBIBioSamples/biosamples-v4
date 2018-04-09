package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.mongodb.core.MongoOperations;

import uk.ac.ebi.biosamples.mongo.model.MongoSample;

public class MongoSampleRepositoryImpl implements MongoSampleRepositoryCustom {

	
	private final MongoOperations mongoOperations;
	
	public MongoSampleRepositoryImpl(MongoOperations mongoOperations) {
		this.mongoOperations = mongoOperations;
		
	}
	
	/**
	 * Uses the MongoOperations.insert() method to only insert new documents
	 * and will throw errors in all other cases.
	 */
	@Override
	public MongoSample insertNew(MongoSample sample) {
		mongoOperations.insert(sample);
		return sample;
	}

}