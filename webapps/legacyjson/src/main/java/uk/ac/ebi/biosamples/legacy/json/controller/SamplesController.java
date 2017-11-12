package uk.ac.ebi.biosamples.legacy.json.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacySample;
import uk.ac.ebi.biosamples.legacy.json.domain.SamplesRelations;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.legacy.json.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/samples", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(LegacySample.class)
public class SamplesController {

    private final PagedResourcesAssembler<LegacySample> pagedResourcesAssembler;
    private final SampleResourceAssembler sampleResourceAssembler;
    private final SampleRepository sampleRepository;
    private final EntityLinks entityLinks;

    @Autowired
    public SamplesController(PagedResourcesAssembler<LegacySample> pagedResourcesAssembler, SampleRepository sampleRepository,
                             SampleResourceAssembler sampleResourceAssembler, EntityLinks entityLinks) {

        this.entityLinks = entityLinks;
        this.sampleRepository = sampleRepository;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.sampleResourceAssembler = sampleResourceAssembler;

    }

    @GetMapping
    public PagedResources<Resource<LegacySample>> allSamplesRelations(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {

        PagedResources<Resource<Sample>> samples = sampleRepository.findSamples(page, size);
        List<LegacySample> legacyRelationsResources = samples.getContent().stream()
                .map(Resource::getContent)
                .map(LegacySample::new)
                .collect(Collectors.toList());
        Pageable pageRequest = new PageRequest(page, size);
        Page<LegacySample> pageResources = new PageImpl<>(legacyRelationsResources, pageRequest, samples.getMetadata().getTotalElements());

        PagedResources<Resource<LegacySample>> pagedResources = pagedResourcesAssembler.toResource(pageResources,
                this.sampleResourceAssembler,
                entityLinks.linkToCollectionResource(SamplesRelations.class));

        pagedResources.add(linkTo(methodOn(SamplesController.class).searchMethods()).withRel("search"));

        return pagedResources;
    }

    @GetMapping("/search")
    public Resources searchMethods() {
        Resources resources = Resources.wrap(Collections.emptyList());
        resources.add(linkTo(methodOn(this.getClass()).searchMethods()).withSelfRel());
        resources.add(linkTo(methodOn(this.getClass()).findFirstSampleContainedInAGroup(null)).withRel("findFirstByGroupsContains"));
        resources.add(linkTo(methodOn(this.getClass()).findByGroups(null, null, null, null)).withRel("findByGroups"));
        resources.add(linkTo(methodOn(this.getClass()).findByAccession(null, null, null, null)).withRel("findByAccession"));
        resources.add(linkTo(methodOn(this.getClass()).findByText(null, null, null, null)).withRel("findByText"));
        resources.add(linkTo(methodOn(this.getClass()).findByTextAndGroups(null, null,  null, null, null)).withRel("findByTextAndGroups"));
        resources.add(linkTo(methodOn(this.getClass()).findByAccessionAndGroups(null, null, null, null, null)).withRel("findByAccessionAndGroups"));

        return resources;
    }

    @GetMapping("/search/findFirstByGroupsContains")
    public ResponseEntity<Resource<LegacySample>> findFirstSampleContainedInAGroup(
            @RequestParam(value="group", required=false, defaultValue = "") String group) {
        Optional<Resource<Sample>> optionalSampleResource = sampleRepository.findFirstByGroup(group);

        if (!optionalSampleResource.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(sampleResourceAssembler.toResource(new LegacySample(optionalSampleResource.get().getContent())));
    }

    @GetMapping("/search/findByGroups")
    public PagedResources<Resource<LegacySample>> findByGroups(
            @RequestParam(value="group", required=false, defaultValue = "") String groupAccession,
            @RequestParam(value="size", required=false, defaultValue = "50") Integer pageSize,
            @RequestParam(value="page", required=false, defaultValue = "0") Integer page,
            @RequestParam(value="sort", required=false, defaultValue = "asc") String sort) {

        PagedResources<Resource<Sample>> samplePagedResources = sampleRepository.findSamplesByGroup(groupAccession, page, pageSize);
        List<LegacySample> legacyRelationsResources = samplePagedResources.getContent().stream()
                .map(Resource::getContent)
                .map(LegacySample::new)
                .collect(Collectors.toList());
        Pageable pageable = new PageRequest(page, pageSize);
        Page<LegacySample> pageRequest = new PageImpl<>(legacyRelationsResources, pageable, samplePagedResources.getMetadata().getTotalElements());
        PagedResources<Resource<LegacySample>> pagedResource = pagedResourcesAssembler.toResource(pageRequest, sampleResourceAssembler);
        return pagedResource;
    }

    @GetMapping("/search/findByAccession")
    public PagedResources findByAccession(
            @RequestParam(value="accession", required=false, defaultValue = "") String accession,
            @RequestParam(value="size", required=false, defaultValue = "50") Integer size,
            @RequestParam(value="page", required=false, defaultValue = "0") Integer page,
            @RequestParam(value="sort", required=false, defaultValue = "asc") String sort
    ) {
        // FIXME This method is always returning empty content in v3
        return pagedResourcesAssembler.toEmptyResource(new PageImpl<>(new ArrayList<>(), new PageRequest(page,size), 0), LegacySample.class, null);
    }


    @GetMapping("/search/findByText")
    public PagedResources<Resource<LegacySample>> findByText(
            @RequestParam(value="text", required=false, defaultValue = "*:*") String text,
            @RequestParam(value="size", defaultValue = "50") Integer pageSize,
            @RequestParam(value="page", defaultValue = "0") Integer pageRequest,
            @RequestParam(value="sort", defaultValue = "asc") String sort
    ) {
        return null;
    }
    @GetMapping("/search/findByTextAndGroups")
    public PagedResources<Resource<LegacySample>> findByTextAndGroups(
            @RequestParam(value="text", required=false, defaultValue = "") String text,
            @RequestParam(value="group", required=false, defaultValue = "") String groupAccession,
            @RequestParam(value="size", defaultValue = "50") Integer pageSize,
            @RequestParam(value="page", defaultValue = "0") Integer pageRequest,
            @RequestParam(value="sort", defaultValue = "asc") String sort
    ) {
        return null;
    }
    @GetMapping("/search/findByAccessionAndGroups")
    public PagedResources<Resource<LegacySample>> findByAccessionAndGroups(
            @RequestParam(value="accession", required=false, defaultValue = "") String accession,
            @RequestParam(value="group", required=false, defaultValue = "") String groupAccession,
            @RequestParam(value="size", defaultValue = "50") Integer pageSize,
            @RequestParam(value="page", defaultValue = "0") Integer pageRequest,
            @RequestParam(value="sort", defaultValue = "asc") String sort
    ) {
        return null;
    }


}
