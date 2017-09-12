package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import uk.ac.ebi.biosamples.mongo.model.MongoCurationLink;
import uk.ac.ebi.biosamples.mongo.model.MongoSubmission;

public interface MongoCurationLinkRepository extends MongoRepository<MongoCurationLink, String> {
	
	Page<MongoCurationLink> findBySample(String sample, Pageable page);
	Page<MongoCurationLink> findByCurationHash(String hash, Pageable page);
}
