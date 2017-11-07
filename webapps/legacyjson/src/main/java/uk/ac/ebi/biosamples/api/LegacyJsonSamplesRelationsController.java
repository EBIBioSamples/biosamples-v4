package uk.ac.ebi.biosamples.api;

import org.springframework.hateoas.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.model.LegacyGroupsRelations;
import uk.ac.ebi.biosamples.model.LegacyRelations;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.LegacyGroupsRelationsResourceAssembler;
import uk.ac.ebi.biosamples.service.LegacyRelationsResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/samplesrelations", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(LegacyRelations.class)
public class LegacyJsonSamplesRelationsController {

    private final SampleService sampleService;
    private final LegacyRelationsResourceAssembler relationsResourceAssembler;
    private final LegacyGroupsRelationsResourceAssembler groupsRelationsResourceAssembler;

    public LegacyJsonSamplesRelationsController(SampleService sampleService,
                                                LegacyRelationsResourceAssembler relationsResourceAssembler,
                                                LegacyGroupsRelationsResourceAssembler groupsRelationsResourceAssembler) {
        this.sampleService = sampleService;
        this.relationsResourceAssembler = relationsResourceAssembler;
        this.groupsRelationsResourceAssembler = groupsRelationsResourceAssembler;
    }

    @GetMapping("/{accession}")
    public Resource<LegacyRelations> relationsOfSample(@PathVariable String accession) {
        Sample sample = sampleService.findByAccession(accession);
        return relationsResourceAssembler.toResource(new LegacyRelations(sample));
    }

    @GetMapping("/{accession}/groups")
    public Resources<LegacyGroupsRelations> getSamplesGroupRelations(@PathVariable String accession) {
        Sample sample = sampleService.findByAccession(accession);
        List<LegacyGroupsRelations> associatedGroups = sample.getRelationships().stream().filter(r ->
                    r.getTarget().equalsIgnoreCase(accession) && r.getType().equals("has member")
                )
                .map(Relationship::getSource)
                .map(sampleService::findByAccession)
                .map(LegacyGroupsRelations::new)
                .collect(Collectors.toList());

        Link selfLink = linkTo(methodOn(this.getClass()).getSamplesGroupRelations(accession)).withSelfRel();
        return new Resources<>(associatedGroups, selfLink);

    }

}
