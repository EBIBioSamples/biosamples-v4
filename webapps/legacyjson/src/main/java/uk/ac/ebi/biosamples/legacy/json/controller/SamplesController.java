package uk.ac.ebi.biosamples.legacy.json.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacySample;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.legacy.json.service.PagedResourcesConverter;
import uk.ac.ebi.biosamples.legacy.json.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.Collections;
import java.util.Optional;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/samples", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(LegacySample.class)
public class SamplesController {

    private final SampleRepository sampleRepository;
    private final SampleResourceAssembler sampleResourceAssembler;
    private final PagedResourcesConverter pagedResourcesConverter;

    @Autowired
    public SamplesController(SampleRepository sampleRepository,
                             SampleResourceAssembler sampleResourceAssembler,
                             PagedResourcesConverter pagedResourcesConverter) {

        this.sampleRepository = sampleRepository;
        this.sampleResourceAssembler = sampleResourceAssembler;
        this.pagedResourcesConverter = pagedResourcesConverter;
    }

    @GetMapping
    public PagedResources<Resource<LegacySample>> allSamplesRelations(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {

        PagedResources<Resource<Sample>> samples = sampleRepository.findSamples(page, size);
        PagedResources<Resource<LegacySample>> pagedResources = pagedResourcesConverter.toLegacySamplesPagedResource(samples);
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
            @RequestParam(value="size", required=false, defaultValue = "50") Integer size,
            @RequestParam(value="page", required=false, defaultValue = "0") Integer page,
            @RequestParam(value="sort", required=false, defaultValue = "asc") String sort) {

        PagedResources<Resource<Sample>> samplePagedResources = sampleRepository.findSamplesByGroup(groupAccession, page, size);
        return pagedResourcesConverter.toLegacySamplesPagedResource(samplePagedResources);
    }

    @GetMapping("/search/findByAccession")
    public PagedResources findByAccession(
            @RequestParam(value="accession", required=false, defaultValue = "") String accession,
            @RequestParam(value="size", required=false, defaultValue = "50") Integer size,
            @RequestParam(value="page", required=false, defaultValue = "0") Integer page,
            @RequestParam(value="sort", required=false, defaultValue = "asc") String sort
    ) {
        // FIXME This method is always returning empty content in v3
        return pagedResourcesConverter.toLegacySamplesPagedResource(null);
    }


    @GetMapping("/search/findByText")
    public PagedResources<Resource<LegacySample>> findByText(
            @RequestParam(value="text", required=false, defaultValue = "*:*") String text,
            @RequestParam(value="size", required=false, defaultValue = "50") Integer size,
            @RequestParam(value="page", required=false, defaultValue = "0") Integer page,
            @RequestParam(value="sort", defaultValue = "asc") String sort
    ) {
        PagedResources<Resource<Sample>> samplesPagedResourcesByText = sampleRepository.findByText(text, page, size);
        return pagedResourcesConverter.toLegacySamplesPagedResource(samplesPagedResourcesByText);

    }
    @GetMapping("/search/findByTextAndGroups")
    public PagedResources<Resource<LegacySample>> findByTextAndGroups(
            @RequestParam(value="text", required=false, defaultValue = "") String text,
            @RequestParam(value="group", required=false, defaultValue = "") String groupAccession,
            @RequestParam(value="size", defaultValue = "50") Integer size,
            @RequestParam(value="page", defaultValue = "0") Integer page,
            @RequestParam(value="sort", defaultValue = "asc") String sort
    ) {
        PagedResources<Resource<Sample>> samples = sampleRepository.findSamplesByTextAndGroup(
                text,
                groupAccession,
                page,
                size);
        return pagedResourcesConverter.toLegacySamplesPagedResource(samples);
    }

    @GetMapping("/search/findByAccessionAndGroups")
    public PagedResources<Resource<LegacySample>> findByAccessionAndGroups(
            @RequestParam(value="accession", required=false, defaultValue = "") String accession,
            @RequestParam(value="group", required=false, defaultValue = "") String groupAccession,
            @RequestParam(value="size", defaultValue = "50") Integer size,
            @RequestParam(value="page", defaultValue = "0") Integer page,
            @RequestParam(value="sort", defaultValue = "asc") String sort
    ) {
        PagedResources<Resource<Sample>> sample = sampleRepository.findSampleInGroup(
                accession, groupAccession);
        return pagedResourcesConverter.toLegacySamplesPagedResource(sample);
    }


}
