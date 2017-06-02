package uk.ac.ebi.biosamples.neo.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Depth;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import uk.ac.ebi.biosamples.neo.model.NeoCuration;

public interface NeoCurationRepository extends Neo4jRepository<NeoCuration, String> {

	public NeoCuration findOneByHash(String hash, @Depth int depth);
	public NeoCuration findOneById(Long id, @Depth int depth);

	@Query("MATCH (s:Sample)--(:CurationLink)--(c:Curation) WHERE s.accession={accession} RETURN c")
	public Page<NeoCuration> findBySampleAccession(@Param("accession") String accession, Pageable pageable, @Depth int depth);
}
