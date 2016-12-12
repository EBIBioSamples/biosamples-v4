package uk.ac.ebi.biosamples.repos.mongo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.biosamples.models.MongoSample;

public interface MongoSampleRepository extends MongoRepository<MongoSample, String> {

	public Page<MongoSample> findByAccession(String accession, Pageable pageable);
}
