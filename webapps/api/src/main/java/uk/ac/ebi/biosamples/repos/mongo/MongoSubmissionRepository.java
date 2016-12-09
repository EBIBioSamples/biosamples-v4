package uk.ac.ebi.biosamples.repos.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import uk.ac.ebi.biosamples.models.MongoSubmission;

//TODO add a @NoRestRepository annotation
public interface MongoSubmissionRepository extends MongoRepository<MongoSubmission, String> {
	
}
