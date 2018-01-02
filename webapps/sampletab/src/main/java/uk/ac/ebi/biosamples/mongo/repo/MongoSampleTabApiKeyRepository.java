package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.mongodb.repository.MongoRepository;

import uk.ac.ebi.biosamples.mongo.model.MongoSampleTabApiKey;

public interface MongoSampleTabApiKeyRepository extends MongoRepository<MongoSampleTabApiKey, String> {

}
