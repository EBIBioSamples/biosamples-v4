package uk.ac.ebi.biosamples.mongo.repo;

import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import uk.ac.ebi.biosamples.mongo.model.MongoSample;

public interface MongoSampleRepository extends MongoRepository<MongoSample, String>, MongoSampleRepositoryCustom {

	Page<MongoSample> findByExternalReferences_Hash(String urlHash, Pageable pageable);

	@Query("{ $and : [{ 'domain' : ?0 },{'name' : ?1 }]}")
	List<MongoSample> findByDomainAndName(String domain, String name);
	
	@Query("{ $and : [{ accessionPrefix : ?0 },{accessionNumber : { $gte : ?1 }}]}")
	Stream<MongoSample> findByAccessionPrefixIsAndAccessionNumberGreaterThanEqual(String accessionPrefix, int accessionNumber, Sort sort);
}
