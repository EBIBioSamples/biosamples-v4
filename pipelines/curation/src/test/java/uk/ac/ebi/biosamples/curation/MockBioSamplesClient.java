package uk.ac.ebi.biosamples.curation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.Resource;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.mockito.Mockito.mock;

public class MockBioSamplesClient extends BioSamplesClient {

    private Logger log = LoggerFactory.getLogger(getClass());

    private Map<String, List<Curation>> curations = new HashMap<>();

    public MockBioSamplesClient(BioSamplesProperties bioSamplesProperties) throws URISyntaxException {
        super(new URI("http://localhost"), new RestTemplateBuilder(), null, null, bioSamplesProperties);
    }

    public Resource<CurationLink> persistCuration(String accession, Curation curation, String domain) {
        log.trace("Persisting curation " + curation + " on " + accession + " in " + domain);
        List<Curation> sampleCurations = curations.get(accession);
        if (sampleCurations == null) {
            sampleCurations = new ArrayList<>();
        }
        sampleCurations.add(curation);
        curations.put(accession, sampleCurations);
        return mock(Resource.class);
    }


    public Collection<Curation> getCurations(String accession) {
        return curations.get(accession);
    }
}
