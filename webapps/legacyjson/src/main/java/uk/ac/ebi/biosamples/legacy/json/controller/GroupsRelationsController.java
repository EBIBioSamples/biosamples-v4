package uk.ac.ebi.biosamples.legacy.json.controller;

import org.springframework.hateoas.*;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.hateoas.core.EmbeddedWrappers;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/groupsrelations", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(GroupsRelations.class)
public class GroupsRelationsController {

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

    @GetMapping
    public PagedResources<Resource<GroupsRelations>> getIndex(
            @RequestParam(value="page", required = false, defaultValue = "0") int page,
            @RequestParam(value="size", required=false, defaultValue = "50") int size,
            @RequestParam(value="sort", required=false, defaultValue = "asc") String sort ) {

        PagedResources<Resource<Sample>> groups = sampleRepository.findGroups(page, size);
        return this.pagedResourcesConverter.toGroupsRelationsPagedResource(groups);

    }


    @GetMapping("/{accession:SAMEG\\d+}")
    public ResponseEntity<Resource<GroupsRelations>> getGroupsRelationsByAccession(@PathVariable String accession) {
        Optional<Sample> sample = sampleRepository.findByAccession(accession);
        if(!sample.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(groupRelationsResourceAssembler.toResource(new GroupsRelations(sample.get())));
    }


    @GetMapping("/{accession:SAMEG\\d+}/samples")
    public ResponseEntity<Resources<SamplesRelations>> getGroupSamplesRelations(@PathVariable String accession) {
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

    @GetMapping("/{accession:SAMEG\\d+}/externalLinks")
    public ResponseEntity<Resources<ExternalLinksRelation>> getGroupExternalLinks(@PathVariable String accession) {
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
