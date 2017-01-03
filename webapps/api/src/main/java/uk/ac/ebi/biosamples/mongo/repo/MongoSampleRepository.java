package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import uk.ac.ebi.biosamples.mongo.model.MongoSample;

@RepositoryRestResource(collectionResourceRel = "samples", path = "samples", itemResourceRel="sample")
public interface MongoSampleRepository extends MongoRepository<MongoSample, String> {
	
}
