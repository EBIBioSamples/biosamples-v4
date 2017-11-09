package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Sample;

import java.time.Instant;
import java.util.List;

@Service
public class SampleRepository {

    public Sample findByAccession(String accession) {

        return Sample.build("test",
                "SAMEA1111",
                "testDomain",
                Instant.now(),
                Instant.now(),
                null, null, null);
    }

    public List<Sample> getSamples() {
        return null;
    }

}
