package uk.ac.ebi.biosamples.mongo.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import uk.ac.ebi.biosamples.mongo.model.MongoSampleTab;

import java.util.List;

public interface MongoSampleTabRepository extends MongoRepository<MongoSampleTab, String>, MongoSampleTabRepositoryCustom {
	
	@Query("{ accessions : ?0 }")
	public List<MongoSampleTab> findByAccessionsContaining(String accession);

}
