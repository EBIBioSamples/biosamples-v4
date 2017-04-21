package uk.ac.ebi.biosamples.neo.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoSample;

public interface NeoExternalReferenceRepository extends Neo4jRepository<NeoExternalReference,String> {

	public NeoExternalReference findOneByUrlHash(String accession);
	public NeoExternalReference findOneById(Long id);
	
	@Query("MATCH (s:Sample)-[:HAS_EXTERNAL_REFERENCE]-(x:ExternalReference) WHERE s.accession={accession} RETURN x")
	public Page<NeoExternalReference> findBySampleAccession(@Param("accession") String accession, Pageable pageable);
	
}
