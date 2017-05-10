package uk.ac.ebi.biosamples.neo.repo;

import org.springframework.data.neo4j.annotation.Depth;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import uk.ac.ebi.biosamples.neo.model.NeoCuration;

public interface NeoCurationRepository extends Neo4jRepository<NeoCuration,String> {

	public NeoCuration findOneByHash(String hash, @Depth int depth);
	public NeoCuration findOneById(Long id, @Depth int depth);

}
