package uk.ac.ebi.biosamples.repos;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import uk.ac.ebi.biosamples.models.NeoSample;

@RepositoryRestResource(path="samplesrelations",collectionResourceRel="samplesrelations",itemResourceRel="samplerelations")
public interface ReadOnlyNeoSampleRepository extends ReadOnlyGraphRepository<NeoSample> {
	
}
