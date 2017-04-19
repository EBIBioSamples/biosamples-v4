package uk.ac.ebi.biosamples.neo.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoSample;

public interface NeoExternalReferenceRepository extends Neo4jRepository<NeoExternalReference,String> {

	public NeoExternalReference findOneByUrlHash(String accession);
	public NeoExternalReference findOneById(Long id);
	
}
