package uk.ac.ebi.biosamples.neo.repo;

import org.springframework.data.neo4j.repository.GraphRepository;

import uk.ac.ebi.biosamples.neo.model.NeoSample;

public interface NeoSampleRepository extends GraphRepository<NeoSample>{

	NeoSample findByAccession(String accession);
	
}
