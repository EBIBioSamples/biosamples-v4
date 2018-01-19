package uk.ac.ebi.biosamples.legacy.json.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.legacy.json.domain.LegacyGroup;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.legacy.json.service.GroupResourceAssembler;
import uk.ac.ebi.biosamples.legacy.json.service.PagedResourcesConverter;
import uk.ac.ebi.biosamples.model.Sample;

@RestController
@RequestMapping(value = "/groups", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(LegacyGroup.class)
public class GroupsController {

    private final SampleRepository sampleRepository;
    private final PagedResourcesConverter pagedResourcesConverter;
    private final GroupResourceAssembler groupResourceAssembler;

    @Autowired
    public GroupsController(SampleRepository sampleRepository,
                            PagedResourcesConverter pagedResourcesConverter,
                            GroupResourceAssembler groupResourceAssembler) {

        this.sampleRepository = sampleRepository;
        this.pagedResourcesConverter = pagedResourcesConverter;
        this.groupResourceAssembler = groupResourceAssembler;
    }

	@CrossOrigin
    @GetMapping
    public PagedResources<Resource<LegacyGroup>> allGroups(
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
            @RequestParam(value = "sort", required = false, defaultValue = "asc") String sort) {

        PagedResources<Resource<Sample>> groups = sampleRepository.findGroups(page, size);
        PagedResources<Resource<LegacyGroup>> pagedResources = pagedResourcesConverter.toLegacyGroupsPagedResource(groups);
        pagedResources.add(linkTo(methodOn(GroupsSearchController.class).searchMethods()).withRel("search"));

        return pagedResources;
    }

	@CrossOrigin
    @GetMapping(value = "/{accession:SAMEG\\d+}")
    public ResponseEntity<Resource<LegacyGroup>> sampleByAccession(@PathVariable String accession) throws InstantiationException {

        Optional<Sample> sample = sampleRepository.findByAccession(accession);
        if (!sample.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        LegacyGroup legacyGroup = new LegacyGroup(sample.get());
        return ResponseEntity.ok(groupResourceAssembler.toResource(legacyGroup));

    }


}
