package uk.ac.ebi.biosamples.repos.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import uk.ac.ebi.biosamples.models.MongoSubmission;

@RepositoryRestResource(exported=false)
public interface MongoSubmissionRepository extends MongoRepository<MongoSubmission, String> {
}
