package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.mongodb.repository.MongoRepository;

import uk.ac.ebi.biosamples.mongo.model.MongoCuration;

public interface MongoCurationRepository extends MongoRepository<MongoCuration, String> {
}
