package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import uk.ac.ebi.biosamples.mongo.model.MongoSubmission;

@RepositoryRestResource(exported=false)
public interface MongoSubmissionRepository extends MongoRepository<MongoSubmission, String> {
}
