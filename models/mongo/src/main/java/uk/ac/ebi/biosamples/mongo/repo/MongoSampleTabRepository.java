package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import uk.ac.ebi.biosamples.mongo.model.MongoSampleTab;

public interface MongoSampleTabRepository extends MongoRepository<MongoSampleTab, String>, MongoSampleTabRepositoryCustom {
	
	@Query("{ accessions : ?0 }")
	MongoSampleTab findOneByAccessionContaining(String accession);
	
}
