package uk.ac.ebi.biosamples.repos.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import uk.ac.ebi.biosamples.models.MongoSample;

@RepositoryRestResource(collectionResourceRel = "samples", path = "samples", itemResourceRel="sample")
public interface MongoSampleRepository extends MongoRepository<MongoSample, String> {
	
	/**
	 * Return a single sample where the accession matches.
	 * 
	 * @param accession
	 * @return
	 */
	public MongoSample findOneByAccession(String accession);	
}
