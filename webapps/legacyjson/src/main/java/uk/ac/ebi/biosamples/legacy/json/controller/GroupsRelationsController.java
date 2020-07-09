package uk.ac.ebi.biosamples.legacy.json.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.hateoas.core.EmbeddedWrappers;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.legacy.json.domain.ExternalLinksRelation;
import uk.ac.ebi.biosamples.legacy.json.domain.GroupsRelations;
import uk.ac.ebi.biosamples.legacy.json.domain.SamplesRelations;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.legacy.json.service.ExternalLinksResourceAssembler;
import uk.ac.ebi.biosamples.legacy.json.service.GroupRelationsResourceAssembler;
import uk.ac.ebi.biosamples.legacy.json.service.PagedResourcesConverter;
import uk.ac.ebi.biosamples.legacy.json.service.SampleRelationsResourceAssembler;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@RestController
@RequestMapping(value = "/groupsrelations", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(GroupsRelations.class)
public class GroupsRelationsController {
    private Logger log = LoggerFactory.getLogger(getClass());

    private final SampleRepository sampleRepository;
    private final GroupRelationsResourceAssembler groupRelationsResourceAssembler;
    private final SampleRelationsResourceAssembler sampleRelationsResourceAssembler;
    private final ExternalLinksResourceAssembler externalLinksResourceAssembler;
    private final PagedResourcesConverter pagedResourcesConverter;

    public GroupsRelationsController(SampleRepository sampleRepository,
                                     ExternalLinksResourceAssembler externalLinksResourceAssembler,
                                     GroupRelationsResourceAssembler groupRelationsResourceAssembler,
                                     SampleRelationsResourceAssembler sampleRelationsResourceAssembler, PagedResourcesConverter pagedResourcesConverter) {

        this.sampleRepository = sampleRepository;
        this.externalLinksResourceAssembler = externalLinksResourceAssembler;
        this.groupRelationsResourceAssembler = groupRelationsResourceAssembler;
        this.sampleRelationsResourceAssembler = sampleRelationsResourceAssembler;
        this.pagedResourcesConverter = pagedResourcesConverter;
    }

	@CrossOrigin
    @GetMapping
    public PagedResources<Resource<GroupsRelations>> allGroupsRelations(
            @RequestParam(value="page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value="size", required = false, defaultValue = "50") Integer size,
            @RequestParam(value="sort", required = false, defaultValue = "asc") String sort ) {
        log.warn("ACCESSING DEPRECATED API at GroupsRelationsController /");

        PagedResources<Resource<Sample>> groups = sampleRepository.findGroups(page, size);
        PagedResources<Resource<GroupsRelations>> groupsRelations = this.pagedResourcesConverter.toGroupsRelationsPagedResource(groups);
        groupsRelations.add(linkTo(methodOn(this.getClass()).search()).withRel("search"));
        return groupsRelations;

    }

	@CrossOrigin
    @GetMapping("/search")
    public Resources search() {
        log.warn("ACCESSING DEPRECATED API at GroupsRelationsController /search");

        Resources searchResources = Resources.wrap(Collections.emptyList());
        searchResources.add(linkTo(methodOn(this.getClass()).search()).withSelfRel());
        searchResources.add(linkTo(methodOn(this.getClass()).findOneByAccession(null)).withRel("findOneByAccession"));
        return searchResources;

    }

	@CrossOrigin
    @GetMapping("/search/findOneByAccession")
    public ResponseEntity<Resource<GroupsRelations>> findOneByAccession(@RequestParam(value = "accession") String accession) {
        log.warn("ACCESSING DEPRECATED API at GroupsRelationsController /{accession:SAMEG\\d+}");
        return this.getGroupsRelationsByAccession(accession);
    }

	@CrossOrigin
    @GetMapping("/{accession:SAMEG\\d+}")
    public ResponseEntity<Resource<GroupsRelations>> getGroupsRelationsByAccession(@PathVariable String accession) {
        log.warn("ACCESSING DEPRECATED API at GroupsRelationsController /");
        Optional<Sample> sample = sampleRepository.findByAccession(accession);
        if(!sample.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(groupRelationsResourceAssembler.toResource(new GroupsRelations(sample.get())));
    }


	@CrossOrigin
    @GetMapping("/{accession:SAMEG\\d+}/samples")
    public ResponseEntity<Resources<SamplesRelations>> getGroupSamplesRelations(@PathVariable String accession) {
        log.warn("ACCESSING DEPRECATED API at GroupsRelationsController /{accession:SAMEG\\d+}/samples");
        Optional<Sample> group = sampleRepository.findByAccession(accession);
        if(!group.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        List<Resource> associatedSamples = group.get().getRelationships().stream()
                .map(Relationship::getTarget)
                .map(sampleRepository::findByAccession)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(SamplesRelations::new)
                .map(sampleRelationsResourceAssembler::toResource)
                .collect(Collectors.toList());

        Link selfLink = linkTo(methodOn(this.getClass()).getGroupSamplesRelations(accession)).withSelfRel();
        Resources responseBody = new Resources(wrappedCollection(associatedSamples, SamplesRelations.class), selfLink);
        return ResponseEntity.ok(responseBody);
    }

	@CrossOrigin
    @GetMapping("/{accession:SAMEG\\d+}/externalLinks")
    public ResponseEntity<Resources<ExternalLinksRelation>> getGroupExternalLinks(@PathVariable String accession) {
        log.warn("ACCESSING DEPRECATED API at GroupsRelationsController /{accession:SAMEG\\d+}/externalLinks");
        Optional<Sample> group = sampleRepository.findByAccession(accession);
        if(!group.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        List<Resource> associatedSamples = group.get().getExternalReferences().stream()
                .map(ExternalReference::getUrl)
                .map(ExternalLinksRelation::new)
                .map(externalLinksResourceAssembler::toResource)
                .collect(Collectors.toList());

        Link selfLink = linkTo(methodOn(this.getClass()).getGroupSamplesRelations(accession)).withSelfRel();
        Resources responseBody = new Resources(wrappedCollection(associatedSamples, ExternalLinksRelation.class), selfLink);
        return ResponseEntity.ok(responseBody);
    }



    /**
     * Wrap the collection and return an empty _embedded if provided collection is empty
     * @param resourceCollection The collection to wrap
     * @param collectionClass the class to use to create the empty collection wrapper
     * @return A resource collection
     */
    private Collection wrappedCollection(List<Resource> resourceCollection, Class collectionClass) {
        EmbeddedWrappers wrappers = new EmbeddedWrappers(false);
        EmbeddedWrapper wrapper;
        if (resourceCollection.isEmpty())
            wrapper = wrappers.emptyCollectionOf(collectionClass);
        else
            wrapper = wrappers.wrap(resourceCollection);
        return Collections.singletonList(wrapper);
    }
}
