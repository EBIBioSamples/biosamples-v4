package uk.ac.ebi.biosamples.repos.ro;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import uk.ac.ebi.biosamples.models.NeoRelationship;

@RepositoryRestResource(exported=false)
public interface ReadOnlyNeoRelationshipRepository extends ReadOnlyGraphRepository<NeoRelationship> {
	
}
