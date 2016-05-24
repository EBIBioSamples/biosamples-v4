package uk.ac.ebi.biosamples.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import uk.ac.ebi.biosamples.models.MongoSample;

public interface MongoSampleRepository extends MongoRepository<MongoSample, String> {

//	public MongoSample findByAccession(String accession);
}
