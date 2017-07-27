package uk.ac.ebi.biosamples.mongo.repo;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import uk.ac.ebi.biosamples.mongo.model.MongoCuration;
import uk.ac.ebi.biosamples.mongo.model.MongoCurationLink;
import uk.ac.ebi.biosamples.mongo.model.MongoExternalReference;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSubmission;

public interface MongoRelationshipRepository extends MongoRepository<MongoRelationship, String> {
	
	List<MongoRelationship> findAllByTarget(String target);
	List<MongoRelationship> findAllBySource(String source);
}
