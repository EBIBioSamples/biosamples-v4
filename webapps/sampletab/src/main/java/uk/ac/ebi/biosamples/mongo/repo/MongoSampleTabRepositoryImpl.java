package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.mongodb.core.MongoOperations;

import uk.ac.ebi.biosamples.mongo.model.MongoSampleTab;

public class MongoSampleTabRepositoryImpl implements MongoSampleTabRepositoryCustom {
	
	private final MongoOperations mongoOperations;

	public MongoSampleTabRepositoryImpl(MongoOperations mongoOperations) {
		this.mongoOperations = mongoOperations;
	}
	
	/**
	 * Uses the MongoOperations.insert() method to only insert new documents
	 * and will throw errors in all other cases.
	 */
	@Override
	public MongoSampleTab insertNew(MongoSampleTab sampletab) {
		mongoOperations.insert(sampletab);
		return sampletab;
	}

}
