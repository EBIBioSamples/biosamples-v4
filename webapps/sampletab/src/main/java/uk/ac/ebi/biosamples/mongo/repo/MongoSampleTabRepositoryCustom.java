package uk.ac.ebi.biosamples.mongo.repo;

import uk.ac.ebi.biosamples.mongo.model.MongoSampleTab;

public interface MongoSampleTabRepositoryCustom {
	
	public MongoSampleTab insertNew(MongoSampleTab sample);

}
