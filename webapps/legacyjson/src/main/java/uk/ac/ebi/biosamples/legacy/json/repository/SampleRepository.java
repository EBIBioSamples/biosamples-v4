package uk.ac.ebi.biosamples.legacy.json.repository;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;

import java.util.*;

@Service
public class SampleRepository {

    private final BioSamplesClient client;

    private final Filter GROUP_FILTER = FilterBuilder.create().onAccession("SAMEG[0-9]+").build();
    private final Filter SAMPLE_FILTER = FilterBuilder.create().onAccession("SAM(N|D|EA|E)[0-9]+").build();

    public SampleRepository(BioSamplesClient client) {
        this.client = client;
    }

    public Optional<Sample> findByAccession(String accession) {

        return client.fetchSample(accession);
    }

    /**
     * Search for an optional sample resource associated with a group
     * @param groupAccession the group accession
     * @return Optional sample resource
     */
    public Optional<Resource<Sample>> findFirstByGroup(String groupAccession) {

        Filter memberOfGroupFilter = FilterBuilder.create().onInverseRelation("has member").withValue(groupAccession).build();

        PagedResources<Resource<Sample>> resourcePage = client.fetchPagedSampleResource(
                "*:*",
                Collections.singletonList(memberOfGroupFilter),
                0,1);

        if (resourcePage.getContent().isEmpty()) {
            return Optional.empty();
        }

        return resourcePage.getContent().stream().findFirst();

    }


    public PagedResources<Resource<Sample>> findSamplesByGroup(String groupAccession, int page, int size) {
        Filter memberOfGroupFilter = FilterBuilder.create().onInverseRelation("has member").withValue(groupAccession).build();

        return client.fetchPagedSampleResource("*:*",
                Collections.singletonList(memberOfGroupFilter),
                page,
                size);
    }



    public PagedResources<Resource<Sample>> findSamples(int page, int size) {

        return client.fetchPagedSampleResource("*:*",
                Collections.singletonList(SAMPLE_FILTER),
                page,
                size);

    }

    public PagedResources<Resource<Sample>> findGroups(int page, int size) {
        return client.fetchPagedSampleResource("*:*",
                Collections.singletonList(GROUP_FILTER),
                page,
                size);

    }

    public PagedResources<Resource<Sample>> findByText(String text, int page, int size) {
        return client.fetchPagedSampleResource(text,
                Collections.singletonList(SAMPLE_FILTER),
                page,
                size);

    }


}
