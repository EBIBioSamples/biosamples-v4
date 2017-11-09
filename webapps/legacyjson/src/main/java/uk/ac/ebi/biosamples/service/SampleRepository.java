package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.List;
import java.util.Optional;

@Service
public class SampleRepository {

    private final BioSamplesClient client;

    public SampleRepository(BioSamplesClient client) {
        this.client = client;
    }

    public Optional<Sample> findByAccession(String accession) {

        return client.fetchSample(accession);
    }

    public List<Sample> getSamples() {
        return null;
    }

}
