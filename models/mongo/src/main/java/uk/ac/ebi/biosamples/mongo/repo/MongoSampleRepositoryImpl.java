package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.mongodb.core.MongoOperations;
import uk.ac.ebi.biosamples.model.StaticViews;
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

	@Override
	public void insertSampleToCollection(MongoSample sample, StaticViews.MongoSampleStaticViews collectionName) {
		mongoOperations.save(sample, collectionName.getCollectionName());
	}

	@Override
	public MongoSample findSampleFromCollection(String accession, StaticViews.MongoSampleStaticViews collectionName) {
		return mongoOperations.findById(accession, MongoSample.class, collectionName.getCollectionName());
	}
}