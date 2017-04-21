package uk.ac.ebi.biosamples.neo.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import uk.ac.ebi.biosamples.neo.model.NeoSample;

public interface NeoSampleRepository extends Neo4jRepository<NeoSample,String>, NeoSampleRepositoryCustom {

	public NeoSample findOneByAccession(String accession);
	public NeoSample findOneById(Long id);
	
	@Query("MATCH (s:Sample)-[:HAS_EXTERNAL_REFERENCE]-(x:ExternalReference) WHERE x.urlHash={urlHash} RETURN s")
	public Page<NeoSample> findByExternalReferenceUrlHash(@Param("urlHash") String urlHash, Pageable pageable);
	
}
