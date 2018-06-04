package uk.ac.ebi.biosamples.mongo.repo;

import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import uk.ac.ebi.biosamples.mongo.model.MongoSample;

public interface MongoSampleRepository extends MongoRepository<MongoSample, String>, MongoSampleRepositoryCustom {

	Page<MongoSample> findByExternalReferences_Hash(String urlHash, Pageable pageable);

	Page<MongoSample> findByDomainAndName(String domain, String name, Pageable pageable);
	
	Stream<MongoSample> findAllByRelationshipsTarget(String target);
	
	@Query("{ $and : [{ accessionPrefix : ?0 },{accessionNumber : { $gte : ?1 }}]}")
	Stream<MongoSample> findByAccessionPrefixIsAndAccessionNumberGreaterThanEqual(String accessionPrefix, int accessionNumber, Sort sort);
	//Stream<MongoSample> findByAccessionPrefix_And_accessionNumber_GreaterThanEqual_OrderBy_accessionNumber(String accessionPrefix, int accessionNumber);
}
