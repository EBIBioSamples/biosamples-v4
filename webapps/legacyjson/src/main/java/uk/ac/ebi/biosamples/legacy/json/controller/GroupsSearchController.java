package uk.ac.ebi.biosamples.legacy.json.controller;

import org.springframework.hateoas.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacyGroup;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacySample;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.legacy.json.service.GroupResourceAssembler;
import uk.ac.ebi.biosamples.legacy.json.service.PagedResourcesConverter;
import uk.ac.ebi.biosamples.model.Sample;

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
        resources.add(linkTo(methodOn(this.getClass()).findByKeywords(null, null, null, null)).withRel("findByKeywords"));
        resources.add(linkTo(methodOn(this.getClass()).findByAccession(null, null, null, null)).withRel("findByAccession"));

        return resources;
    }

    @GetMapping("/findByKeywords")
    public PagedResources<Resource<LegacyGroup>> findByKeywords(
            @RequestParam(value="keyword") String keyword,
            @RequestParam(value="page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value="size", required = false, defaultValue = "50") Integer size,
            @RequestParam(value="sort", required = false, defaultValue = "asc") String sort)
    {
        PagedResources<Resource<Sample>> groupsByText = sampleRepository.findGroupsByText(keyword, page, size);
        return pagedResourcesConverter.toLegacyGroupsPagedResource(groupsByText);
    }

    @GetMapping("/findByAccession")
    public PagedResources<Resource<LegacyGroup>> findByAccession(
            @RequestParam(value="accession") String accession,
            @RequestParam(value="page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value="size", required = false, defaultValue = "50") Integer size,
            @RequestParam(value="sort", required = false, defaultValue = "asc") String sort)
    {
        return findByKeywords(accession, page, size, sort);
    }
}
