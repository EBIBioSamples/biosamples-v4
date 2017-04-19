package uk.ac.ebi.biosamples.neo.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import uk.ac.ebi.biosamples.neo.model.NeoSample;

public interface NeoSampleRepository extends Neo4jRepository<NeoSample,String>, NeoSampleRepositoryCustom {

	public NeoSample findOneByAccession(String accession);
	public NeoSample findOneById(Long id);
	
}
