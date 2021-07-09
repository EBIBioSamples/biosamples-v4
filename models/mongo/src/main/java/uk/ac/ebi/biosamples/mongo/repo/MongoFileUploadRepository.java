package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.biosamples.mongo.model.MongoFileUpload;

public interface MongoFileUploadRepository extends MongoRepository<MongoFileUpload, String> {
}
