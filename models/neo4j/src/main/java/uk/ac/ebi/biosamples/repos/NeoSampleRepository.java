package uk.ac.ebi.biosamples.repos;

import org.springframework.data.neo4j.repository.GraphRepository;

import uk.ac.ebi.biosamples.models.NeoSample;

public interface NeoSampleRepository extends  GraphRepository<NeoSample>{

	NeoSample findByAccession(String accession);
	
}
