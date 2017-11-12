package uk.ac.ebi.biosamples.legacy.json.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacySample;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PagedResourcesConverter {

    private final PagedResourcesAssembler<LegacySample> legacySamplePagedResourcesAssembler;
    private final SampleResourceAssembler legacySampleResourceAssembler;

    public PagedResourcesConverter(PagedResourcesAssembler<LegacySample> legacySamplePagedResourcesAssembler, SampleResourceAssembler legacySampleResourceAssembler) {
        this.legacySamplePagedResourcesAssembler = legacySamplePagedResourcesAssembler;
        this.legacySampleResourceAssembler = legacySampleResourceAssembler;
    }

    public PagedResources<Resource<LegacySample>> toLegacySamplesPagedResource(PagedResources<Resource<Sample>>
                                                                               samplePagedResources) {
        if (samplePagedResources == null || samplePagedResources.getContent().isEmpty()) {
            return getSamplesEmptyPagedResource();
        }
        List<LegacySample> legacyRelationsResources = samplePagedResources.getContent().stream()
                .map(Resource::getContent)
                .map(LegacySample::new)
                .collect(Collectors.toList());
        Page<LegacySample> pageRequest = buildPageRequest(legacyRelationsResources, samplePagedResources.getMetadata());
        return legacySamplePagedResourcesAssembler.toResource(pageRequest, legacySampleResourceAssembler);

    }

    private PagedResources getSamplesEmptyPagedResource() {
        return legacySamplePagedResourcesAssembler.toEmptyResource(new PageImpl<>(new ArrayList<>(), new PageRequest(0, 50), 0), LegacySample.class, null);
    }

    private <T> Page<T> buildPageRequest(List<T> samples, PagedResources.PageMetadata pageMetadata) {
        int page = (int) pageMetadata.getNumber();
        int size = (int) pageMetadata.getSize();
        long total = pageMetadata.getTotalElements();

        return new PageImpl(samples, new PageRequest(page,size), total);
    }

}
