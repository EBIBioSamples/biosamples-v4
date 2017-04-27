package uk.ac.ebi.biosamples.neo.repo;

import org.springframework.data.neo4j.annotation.Depth;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import uk.ac.ebi.biosamples.neo.model.NeoCuration;
import uk.ac.ebi.biosamples.neo.model.NeoCurationLink;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;

public interface NeoCurationLinkRepository extends Neo4jRepository<NeoCurationLink,String> {

	public NeoCurationLink findOneByHash(String hash, @Depth int depth);
	public NeoCurationLink findOneById(Long id, @Depth int depth);

}
