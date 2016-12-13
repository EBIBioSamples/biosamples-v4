package uk.ac.ebi.biosamples.repos.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import uk.ac.ebi.biosamples.models.MongoSample;

public interface MongoSampleRepository extends MongoRepository<MongoSample, String> {
	
	/**
	 * Return a single sample where the accession matches.
	 * 
	 * @param accession
	 * @return
	 */
	public MongoSample findOneByAccession(String accession);	
}
