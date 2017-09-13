package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.mongodb.repository.MongoRepository;

import uk.ac.ebi.biosamples.mongo.model.MongoExternalReference;

public interface MongoExternalReferenceRepository extends MongoRepository<MongoExternalReference, String> {
}
