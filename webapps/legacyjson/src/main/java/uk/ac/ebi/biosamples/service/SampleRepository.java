package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;

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

    public PagedResources<Resource<Sample>> getPagedSamples(int page, int pageSize) {
        return client.fetchPagedSampleResource("*:*", page, pageSize);
    }

}
