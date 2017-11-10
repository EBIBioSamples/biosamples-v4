package uk.ac.ebi.biosamples.legacy.json.controller;

import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.legacy.json.domain.GroupsRelations;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.legacy.json.service.GroupRelationsResourceAssembler;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;

import java.util.Optional;

@RestController
@RequestMapping(value = "/groupsrelations", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(GroupsRelations.class)
public class GroupsRelationsController {

    private final SampleRepository sampleRepository;
    private final GroupRelationsResourceAssembler resourceAssembler;

    public GroupsRelationsController(SampleRepository sampleRepository, GroupRelationsResourceAssembler resourceAssembler) {
        this.sampleRepository = sampleRepository;
        this.resourceAssembler = resourceAssembler;
    }


    @GetMapping("/{accession}")
    public ResponseEntity<Resource<GroupsRelations>> getGroupsRelationsByAccession(@PathVariable String accession) {
        Optional<Sample> sample = sampleRepository.findByAccession(accession);
        if(!sample.isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(resourceAssembler.toResource(new GroupsRelations(sample.get())));
    }

}
