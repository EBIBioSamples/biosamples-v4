package uk.ac.ebi.biosamples.legacy.json.controller;

import org.springframework.hateoas.*;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.hateoas.core.EmbeddedWrappers;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.legacy.json.domain.GroupsRelations;
import uk.ac.ebi.biosamples.legacy.json.domain.SamplesRelations;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.legacy.json.service.GroupRelationsResourceAssembler;
import uk.ac.ebi.biosamples.legacy.json.repository.RelationsRepository;
import uk.ac.ebi.biosamples.legacy.json.service.SampleRelationsResourceAssembler;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/samplesrelations/", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(SamplesRelations.class)
public class SampleRelationsController {

    private final SampleRepository sampleRepository;
    private final SampleRelationsResourceAssembler relationsResourceAssembler;
    private final GroupRelationsResourceAssembler groupRelationsResourceAssembler;
    private final RelationsRepository relationService;

    public SampleRelationsController(SampleRepository sampleRepository,
                                     SampleRelationsResourceAssembler relationsResourceAssembler,
                                     GroupRelationsResourceAssembler groupRelationsResourceAssembler,
                                     RelationsRepository relationService) {

        this.sampleRepository = sampleRepository;
        this.relationsResourceAssembler = relationsResourceAssembler;
        this.groupRelationsResourceAssembler = groupRelationsResourceAssembler;
        this.relationService = relationService;

    }

    @GetMapping("/{accession}")
    public ResponseEntity<Resource<SamplesRelations>> relationsOfSample(@PathVariable String accession) {
        Optional<Sample> sample = sampleRepository.findByAccession(accession);
        if (!sample.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(relationsResourceAssembler.toResource(new SamplesRelations(sample.get())));
    }

    @GetMapping("/{accession}/groups")
    public ResponseEntity<Resources<GroupsRelations>> getSamplesGroupRelations(@PathVariable String accession) {

        List<Resource> associatedGroups = relationService
                .getGroupsRelationships(accession).stream()
                .map(groupRelationsResourceAssembler::toResource)
                .collect(Collectors.toList());

        Link selfLink = linkTo(methodOn(this.getClass()).getSamplesGroupRelations(accession)).withSelfRel();
        Resources responseBody = new Resources(wrappedCollection(associatedGroups, GroupsRelations.class), selfLink);
        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/{accession}/{relationType}")
    public ResponseEntity<Resources> getSamplesRelations(
            @PathVariable String accession,
            @PathVariable String relationType) {

        if (!relationService.isSupportedRelation(relationType)) {
            return ResponseEntity.badRequest().build();
        }

        List<Resource> associatedSamples = relationService
                .getSamplesRelations(accession, relationType).stream()
                .map(relationsResourceAssembler::toResource)
                .collect(Collectors.toList());

        Link selfLink = linkTo(methodOn(this.getClass()).getSamplesRelations(accession, relationType)).withSelfRel();
        Resources responseBody = new Resources(wrappedCollection(associatedSamples, SamplesRelations.class), selfLink);
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
