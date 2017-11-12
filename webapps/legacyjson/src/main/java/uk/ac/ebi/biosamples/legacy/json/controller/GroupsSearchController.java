package uk.ac.ebi.biosamples.legacy.json.controller;

import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacySample;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.legacy.json.service.GroupResourceAssembler;
import uk.ac.ebi.biosamples.legacy.json.service.PagedResourcesConverter;

import java.util.Collections;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/groups/search", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(LegacySample.class)
public class GroupsSearchController {

    private final SampleRepository sampleRepository;
    private final GroupResourceAssembler sampleResourceAssembler;
    private final PagedResourcesConverter pagedResourcesConverter;

    public GroupsSearchController(SampleRepository sampleRepository,
                                  GroupResourceAssembler groupResourceAssembler,
                                  PagedResourcesConverter pagedResourcesConverter) {

        this.sampleRepository = sampleRepository;
        this.sampleResourceAssembler = groupResourceAssembler;
        this.pagedResourcesConverter = pagedResourcesConverter;
    }


    @GetMapping
    public Resources searchMethods() {
        Resources resources = Resources.wrap(Collections.emptyList());
        resources.add(linkTo(methodOn(this.getClass()).searchMethods()).withSelfRel());
//        resources.add(linkTo(methodOn(this.getClass()).findFirstSampleContainedInAGroup(null)).withRel("findFirstByGroupsContains"));
//        resources.add(linkTo(methodOn(this.getClass()).findByGroups(null, null, null, null)).withRel("findByGroups"));
//        resources.add(linkTo(methodOn(this.getClass()).findByAccession(null, null, null, null)).withRel("findByAccession"));
//        resources.add(linkTo(methodOn(this.getClass()).findByText(null, null, null, null)).withRel("findByText"));
//        resources.add(linkTo(methodOn(this.getClass()).findByTextAndGroups(null, null,  null, null, null)).withRel("findByTextAndGroups"));
//        resources.add(linkTo(methodOn(this.getClass()).findByAccessionAndGroups(null, null, null, null, null)).withRel("findByAccessionAndGroups"));

        return resources;
    }

//    @GetMapping("/findFirstByGroupsContains")
//    public ResponseEntity<Resource<LegacySample>> findFirstSampleContainedInAGroup(
//            @RequestParam(value="group", required=false, defaultValue = "") String group) {
//        Optional<Resource<Sample>> optionalSampleResource = sampleRepository.findFirstByGroup(group);
//
//        if (!optionalSampleResource.isPresent()) {
//            return ResponseEntity.notFound().build();
//        }
//
//        return ResponseEntity.ok(sampleResourceAssembler.toResource(new LegacySample(optionalSampleResource.get().getContent())));
//    }
//
//    @GetMapping("/findByGroups")
//    public PagedResources<Resource<LegacySample>> findByGroups(
//            @RequestParam(value="group", required=false, defaultValue = "") String groupAccession,
//            @RequestParam(value="size", required=false, defaultValue = "50") Integer size,
//            @RequestParam(value="page", required=false, defaultValue = "0") Integer page,
//            @RequestParam(value="sort", required=false, defaultValue = "asc") String sort) {
//
//        PagedResources<Resource<Sample>> samplePagedResources = sampleRepository.findSamplesByGroup(groupAccession, page, size);
//        return pagedResourcesConverter.toLegacySamplesPagedResource(samplePagedResources);
//    }
//
//    @GetMapping("/findByAccession")
//    public PagedResources findByAccession(
//            @RequestParam(value="accession", required=false, defaultValue = "") String accession,
//            @RequestParam(value="size", required=false, defaultValue = "50") Integer size,
//            @RequestParam(value="page", required=false, defaultValue = "0") Integer page,
//            @RequestParam(value="sort", required=false, defaultValue = "asc") String sort
//    ) {
//        // FIXME This method is always returning empty content in v3
//        return pagedResourcesConverter.toLegacySamplesPagedResource(null);
//    }
//
//
//    @GetMapping("/findByText")
//    public PagedResources<Resource<LegacySample>> findByText(
//            @RequestParam(value="text", required=false, defaultValue = "*:*") String text,
//            @RequestParam(value="size", required=false, defaultValue = "50") Integer size,
//            @RequestParam(value="page", required=false, defaultValue = "0") Integer page,
//            @RequestParam(value="sort", defaultValue = "asc") String sort
//    ) {
//        PagedResources<Resource<Sample>> samplesPagedResourcesByText = sampleRepository.findByText(text, page, size);
//        return pagedResourcesConverter.toLegacySamplesPagedResource(samplesPagedResourcesByText);
//
//    }
//    @GetMapping("/findByTextAndGroups")
//    public PagedResources<Resource<LegacySample>> findByTextAndGroups(
//            @RequestParam(value="text", required=false, defaultValue = "") String text,
//            @RequestParam(value="group", required=false, defaultValue = "") String groupAccession,
//            @RequestParam(value="size", defaultValue = "50") Integer size,
//            @RequestParam(value="page", defaultValue = "0") Integer page,
//            @RequestParam(value="sort", defaultValue = "asc") String sort
//    ) {
//        PagedResources<Resource<Sample>> samples = sampleRepository.findSamplesByTextAndGroup(
//                text,
//                groupAccession,
//                page,
//                size);
//        return pagedResourcesConverter.toLegacySamplesPagedResource(samples);
//    }
//
//    @GetMapping("/findByAccessionAndGroups")
//    public PagedResources<Resource<LegacySample>> findByAccessionAndGroups(
//            @RequestParam(value="accession", required=false, defaultValue = "") String accession,
//            @RequestParam(value="group", required=false, defaultValue = "") String groupAccession,
//            @RequestParam(value="size", defaultValue = "50") Integer size,
//            @RequestParam(value="page", defaultValue = "0") Integer page,
//            @RequestParam(value="sort", defaultValue = "asc") String sort
//    ) {
//        PagedResources<Resource<Sample>> sample = sampleRepository.findSampleInGroup(
//                accession, groupAccession);
//        return pagedResourcesConverter.toLegacySamplesPagedResource(sample);
//    }
}
