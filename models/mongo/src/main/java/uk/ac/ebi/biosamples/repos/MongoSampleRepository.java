package uk.ac.ebi.biosamples.repos;

import org.springframework.data.mongodb.repository.MongoRepository;

import uk.ac.ebi.biosamples.models.MongoSample;

public interface MongoSampleRepository extends MongoRepository<MongoSample, String> {

	/**
	 * There should only be one document with a specific accession. Other
	 * versions will have a "previous accession" field.
	 * 
	 * @param accession
	 * @return
	 */
	public MongoSample findByAccession(String accession);

	public Iterable<MongoSample> findAllByPreviousAccession(String accession);
}
