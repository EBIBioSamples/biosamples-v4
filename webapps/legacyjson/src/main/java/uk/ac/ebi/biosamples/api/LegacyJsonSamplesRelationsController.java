package uk.ac.ebi.biosamples.api;

import org.springframework.hateoas.*;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.hateoas.core.EmbeddedWrappers;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.model.LegacyGroupsRelations;
import uk.ac.ebi.biosamples.model.LegacySamplesRelations;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.LegacyGroupsRelationsResourceAssembler;
import uk.ac.ebi.biosamples.service.LegacyRelationService;
import uk.ac.ebi.biosamples.service.LegacySamplesRelationsResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/samplesrelations", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(LegacySamplesRelations.class)
public class LegacyJsonSamplesRelationsController {

    private final SampleService sampleService;
    private final LegacySamplesRelationsResourceAssembler relationsResourceAssembler;
    private final LegacyGroupsRelationsResourceAssembler groupsRelationsResourceAssembler;
    private final LegacyRelationService relationService;

    public LegacyJsonSamplesRelationsController(SampleService sampleService,
                                                LegacyRelationService legacyRelationService,
                                                LegacySamplesRelationsResourceAssembler relationsResourceAssembler,
                                                LegacyGroupsRelationsResourceAssembler groupsRelationsResourceAssembler) {
        this.sampleService = sampleService;
        this.relationService = legacyRelationService;
        this.relationsResourceAssembler = relationsResourceAssembler;
        this.groupsRelationsResourceAssembler = groupsRelationsResourceAssembler;
    }


    @GetMapping("/{accession}")
    public Resource<LegacySamplesRelations> relationsOfSample(@PathVariable String accession) {
        Sample sample = sampleService.findByAccession(accession);
        return relationsResourceAssembler.toResource(new LegacySamplesRelations(sample));
    }

    @GetMapping("/{accession}/groups")
    public ResponseEntity<Resources> getSamplesGroupRelations(@PathVariable String accession) {

        List<Resource<?>> associatedGroups = relationService
                .getGroupsRelationships(accession).stream()
                .map(groupsRelationsResourceAssembler::toResource)
                .collect(Collectors.toList());

        Link selfLink = linkTo(methodOn(this.getClass()).getSamplesGroupRelations(accession)).withSelfRel();
        Resources responseBody = new Resources(wrappedCollection(associatedGroups, LegacyGroupsRelations.class), selfLink);
        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/{accession}/{relationType}")
    public ResponseEntity<Resources> getSamplesRelations(
            @PathVariable String accession,
            @PathVariable String relationType) {

        if (!relationService.isSupportedRelation(relationType)) {
            return ResponseEntity.badRequest().build();
        }

        List<Resource<?>> associatedSamples = relationService
                .getSamplesRelations(accession, relationType).stream()
                .map(relationsResourceAssembler::toResource)
                .collect(Collectors.toList());

        Link selfLink = linkTo(methodOn(this.getClass()).getSamplesRelations(accession, relationType)).withSelfRel();
        Resources responseBody = new Resources(wrappedCollection(associatedSamples, LegacySamplesRelations.class), selfLink);
        return ResponseEntity.ok(responseBody);

    }

    private Collection<?> wrappedCollection(List<Resource<?>> resourceCollection, Class collectionClass) {
        EmbeddedWrappers wrappers = new EmbeddedWrappers(false);
        EmbeddedWrapper wrapper;
        if (resourceCollection.isEmpty())
            wrapper = wrappers.emptyCollectionOf(collectionClass);
        else
            wrapper = wrappers.wrap(resourceCollection);
        return Collections.singletonList(wrapper);
    }

}

