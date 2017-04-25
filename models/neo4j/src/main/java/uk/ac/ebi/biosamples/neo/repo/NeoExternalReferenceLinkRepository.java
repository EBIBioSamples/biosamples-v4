package uk.ac.ebi.biosamples.neo.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Depth;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import uk.ac.ebi.biosamples.neo.model.NeoExternalReference;
import uk.ac.ebi.biosamples.neo.model.NeoExternalReferenceLink;
import uk.ac.ebi.biosamples.neo.model.NeoSample;

public interface NeoExternalReferenceLinkRepository extends Neo4jRepository<NeoExternalReferenceLink,String> {

	public NeoExternalReferenceLink findOneById(String id, @Depth int depth);
	public NeoExternalReferenceLink findOneByHash(String keyHash, @Depth int depth);

	
	
}
