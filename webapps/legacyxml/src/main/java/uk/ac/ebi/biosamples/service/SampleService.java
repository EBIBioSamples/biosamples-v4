package uk.ac.ebi.biosamples.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;

import java.net.URI;

@Service
public class SampleService {

    private BioSamplesClient client;

    @Value("${biosamples.submissionuri}")
    private URI biosampleSubmissionUri;

    public SampleService(BioSamplesClient client) {
        this.client = client;
    }

    public PagedResources<Resource<Sample>> getSamples() {
        return client.fetchPagedSamples(0, 25);
    }

}
