package uk.ac.ebi.biosamples.api;

import org.springframework.hateoas.*;
import org.springframework.hateoas.core.EmbeddedWrapper;
import org.springframework.hateoas.core.EmbeddedWrappers;
import org.springframework.http.MediaType;
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

import java.util.Arrays;
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
    public Resources<LegacyGroupsRelations> getSamplesGroupRelations(@PathVariable String accession) {
        List<LegacyGroupsRelations> associatedGroups = relationService.getGroupsRelationships(accession);

        Link selfLink = linkTo(methodOn(this.getClass()).getSamplesGroupRelations(accession)).withSelfRel();
        if (!associatedGroups.isEmpty()) {
            return new Resources<>(associatedGroups, selfLink);
        }

        EmbeddedWrappers wrappers = new EmbeddedWrappers(false);
        EmbeddedWrapper wrapper = wrappers.emptyCollectionOf(LegacyGroupsRelations.class);
        return new Resources(Arrays.asList(wrapper), selfLink);
    }

    @GetMapping("/{accession}/{relationType}")
    public Resources getSamplesRelations(
            @PathVariable String accession,
            @PathVariable String relationType) {
        List<Resource<LegacySamplesRelations>> associatedSamples = relationService
                .getSamplesRelations(accession, relationType).stream()
                .map(relationsResourceAssembler::toResource)
                .collect(Collectors.toList());

        Link selfLink = linkTo(methodOn(this.getClass()).getSamplesRelations(accession, relationType)).withSelfRel();
        if (!associatedSamples.isEmpty()) {
            return new Resources<>(associatedSamples, selfLink);
        }

        EmbeddedWrappers wrappers = new EmbeddedWrappers(false);
        EmbeddedWrapper wrapper = wrappers.emptyCollectionOf(LegacySamplesRelations.class);
        return new Resources(Arrays.asList(wrapper), selfLink);

    }

}

