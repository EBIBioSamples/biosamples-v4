package uk.ac.ebi.biosamples.legacy.json.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.legacy.json.domain.ExternalLinksRelation;
import uk.ac.ebi.biosamples.legacy.json.domain.GroupsRelations;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacyGroup;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacySample;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PagedResourcesConverter {

    private final PagedResourcesAssembler<GroupsRelations> groupsRelationsPagedResourcesAssembler;
    private final PagedResourcesAssembler<LegacySample> legacySamplePagedResourcesAssembler;
    private final PagedResourcesAssembler<LegacyGroup> legacyGroupPagedResourcesAssembler;
    private final GroupRelationsResourceAssembler groupRelationsResourceAssembler;
    private final SampleResourceAssembler legacySampleResourceAssembler;
    private final GroupResourceAssembler legacyGroupResourceAssembler;

    public PagedResourcesConverter(PagedResourcesAssembler<GroupsRelations> groupsRelationsPagedResourcesAssembler, PagedResourcesAssembler<LegacySample> legacySamplePagedResourcesAssembler,
                                   PagedResourcesAssembler<LegacyGroup> legacyGroupPagedResourcesAssembler,
                                   GroupRelationsResourceAssembler groupRelationsResourceAssembler, SampleResourceAssembler legacySampleResourceAssembler,
                                   GroupResourceAssembler legacyGroupResourceAssembler) {

        this.groupsRelationsPagedResourcesAssembler = groupsRelationsPagedResourcesAssembler;
        this.legacySamplePagedResourcesAssembler = legacySamplePagedResourcesAssembler;
        this.legacyGroupPagedResourcesAssembler = legacyGroupPagedResourcesAssembler;
        this.groupRelationsResourceAssembler = groupRelationsResourceAssembler;
        this.legacySampleResourceAssembler = legacySampleResourceAssembler;
        this.legacyGroupResourceAssembler = legacyGroupResourceAssembler;
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

    public PagedResources<Resource<LegacyGroup>> toLegacyGroupsPagedResource(PagedResources<Resource<Sample>>
                                                                              samplePagedResources) {

        if (samplePagedResources == null || samplePagedResources.getContent().isEmpty()) {
            return getGroupsEmptyPagedResource();
        }
        List<LegacyGroup> legacyRelationsResources = samplePagedResources.getContent().stream()
                .map(Resource::getContent)
                .map(LegacyGroup::new)
                .collect(Collectors.toList());
        Page<LegacyGroup> pageRequest = buildPageRequest(legacyRelationsResources, samplePagedResources.getMetadata());
        return legacyGroupPagedResourcesAssembler.toResource(pageRequest, legacyGroupResourceAssembler);

    }

    public PagedResources<Resource<GroupsRelations>> toGroupsRelationsPagedResource(PagedResources<Resource<Sample>> samplePagedResources) {
        if (samplePagedResources == null || samplePagedResources.getContent().isEmpty()) {
            return getGroupsEmptyPagedResource();
        }

        List<GroupsRelations> legacyRelationsResources = samplePagedResources.getContent().stream()
                .map(Resource::getContent)
                .map(GroupsRelations::new)
                .collect(Collectors.toList());

        Page<GroupsRelations> pageRequest = buildPageRequest(legacyRelationsResources, samplePagedResources.getMetadata());
        return groupsRelationsPagedResourcesAssembler.toResource(pageRequest, groupRelationsResourceAssembler);

    }


    public PagedResources<Resource<ExternalLinksRelation>> toExternalLinksRelationPagedResource(PagedResources<Resource<Sample>>
                                                                                     samplePagedResources) {

//        if (samplePagedResources == null || samplePagedResources.getContent().isEmpty()) {
            return getEmptyPagedResource(ExternalLinksRelation.class);
//        }
//        List<LegacyGroup> legacyRelationsResources = samplePagedResources.getContent().stream()
//                .map(Resource::getContent)
//                .map(LegacyGroup::new)
//                .collect(Collectors.toList());
//        Page<LegacyGroup> pageRequest = buildPageRequest(legacyRelationsResources, samplePagedResources.getMetadata());
//        return legacyGroupPagedResourcesAssembler.toResource(pageRequest, legacyGroupResourceAssembler);

    }


    private PagedResources getEmptyPagedResource(Class className) {
        return legacySamplePagedResourcesAssembler.toEmptyResource(new PageImpl<>(new ArrayList<>(), new PageRequest(0, 50), 0), className, null);

    }

    private PagedResources getSamplesEmptyPagedResource() {
        return legacySamplePagedResourcesAssembler.toEmptyResource(new PageImpl<>(new ArrayList<>(), new PageRequest(0, 50), 0), LegacySample.class, null);
    }

    private PagedResources getGroupsEmptyPagedResource() {
        return legacySamplePagedResourcesAssembler.toEmptyResource(new PageImpl<>(new ArrayList<>(), new PageRequest(0, 50), 0), LegacyGroup.class, null);
    }
    private <T> Page<T> buildPageRequest(List<T> samples, PagedResources.PageMetadata pageMetadata) {
        int page = (int) pageMetadata.getNumber();
        int size = (int) pageMetadata.getSize();
        long total = pageMetadata.getTotalElements();

        return new PageImpl(samples, new PageRequest(page,size), total);
    }

}
