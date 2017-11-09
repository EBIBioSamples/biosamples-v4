package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;

import java.util.Collections;
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

        Filter sampleFilter = FilterBuilder.create().onAccession("SAM(N|D|EA|E)[0-9]+").build();
        return getPagedContent(page, pageSize, sampleFilter);

    }

    public PagedResources<Resource<Sample>> getPagedGroups(int page, int pageSize) {
        Filter groupFilter = FilterBuilder.create().onAccession("SAMEG[0-9]+").build();
        return getPagedContent(page, pageSize, groupFilter);

    }

    private PagedResources<Resource<Sample>> getPagedContent(int page, int pageSize, Filter filter) {
        return client.fetchPagedSampleResource("*:*",
                Collections.singletonList(filter), page, pageSize);
    }

}
