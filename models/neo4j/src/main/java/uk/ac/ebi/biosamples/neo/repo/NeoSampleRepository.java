package uk.ac.ebi.biosamples.neo.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Depth;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import uk.ac.ebi.biosamples.neo.model.NeoSample;

public interface NeoSampleRepository extends Neo4jRepository<NeoSample,String>, NeoSampleRepositoryCustom {

	public NeoSample findOneByAccession(String accession, @Depth int depth);
	public NeoSample findOneById(Long id, @Depth int depth);

	@Query("MATCH (s:Sample)--(:ExternalReferenceLink)--(x:ExternalReference) WHERE x.urlHash={urlHash} RETURN s")
	public Page<NeoSample> findByExternalReferenceUrlHash(@Param("urlHash") String urlHash, Pageable pageable);

	@Query("MATCH (s:Sample)--(l:ExternalReferenceLink) WHERE l.hash={hash} RETURN s")
	public Page<NeoSample> findByExternalReferenceLinkHash(@Param("hash") String hash, Pageable pageable);


	@Query("MATCH (s:Sample)--(:CurationLink)--(x:Curation) WHERE x.hash={hash} RETURN s")
	public Page<NeoSample> findByCurationHash(@Param("hash") String hash, Pageable pageable);

}