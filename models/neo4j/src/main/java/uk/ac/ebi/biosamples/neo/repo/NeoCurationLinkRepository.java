package uk.ac.ebi.biosamples.neo.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Depth;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import uk.ac.ebi.biosamples.neo.model.NeoCurationLink;

public interface NeoCurationLinkRepository extends Neo4jRepository<NeoCurationLink,String> {

	public NeoCurationLink findOneByHash(String hash, @Depth int depth);
	public NeoCurationLink findOneById(Long id, @Depth int depth);

	@Query("MATCH (s:Sample)--(l:CurationLink) WHERE s.accession={accession} RETURN l")
	public Page<NeoCurationLink> findBySampleAccession(@Param("accession") String accession, Pageable pageable);

}
