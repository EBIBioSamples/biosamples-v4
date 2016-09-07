package uk.ac.ebi.biosamples.repos;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import uk.ac.ebi.biosamples.models.NeoSample;


@RepositoryRestResource(path="samplesrelations",collectionResourceRel="samplesrelations",itemResourceRel="samplerelations", exported=true)
//docs say one or the toher should be sufficient, but seems that you do need both
@RestResource(path="samplesrelations",exported=true)
public interface ReadOnlyNeoSampleRepository extends ReadOnlyGraphRepository<NeoSample> {
	
}
