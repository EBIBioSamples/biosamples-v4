package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.mongodb.repository.MongoRepository;

import uk.ac.ebi.biosamples.mongo.model.MongoSubmission;

public interface MongoSubmissionRepository extends MongoRepository<MongoSubmission, String> {
}
