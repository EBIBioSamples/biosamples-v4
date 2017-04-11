package uk.ac.ebi.biosamples.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.Sort;

import java.net.URI;

@Service
public class SampleService {

    private BioSamplesClient client;

    @Value("${biosamples.submissionuri}")
    private URI biosampleSubmissionUri;

    public SampleService(BioSamplesClient client) {
        this.client = client;
    }

    // FIXME The sorting is not implemented in the samples endpoint in the core, do we actually use it?
    public PagedResources<Resource<Sample>> getSamples(String query, int start, int size, Sort sortMethod) {
        return client.fetchPagedSamples(query, start, size);
    }


}
