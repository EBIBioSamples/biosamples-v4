package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.mongodb.repository.MongoRepository;

import uk.ac.ebi.biosamples.mongo.model.MongoSample;

public interface MongoSampleRepository extends MongoRepository<MongoSample, String>, MongoSampleRepositoryCustom {

}
