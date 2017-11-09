package uk.ac.ebi.biosamples.api;

import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.model.LegacyGroupsRelations;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.LegacyGroupsRelationsResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleRepository;

import java.util.Optional;

@RestController
@RequestMapping(value = "/groupsrelations", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(LegacyGroupsRelations.class)
public class LegacyJsonGroupsRelationsController {

    private final SampleRepository sampleRepository;
    private final LegacyGroupsRelationsResourceAssembler resourceAssembler;

    public LegacyJsonGroupsRelationsController(SampleRepository sampleRepository, LegacyGroupsRelationsResourceAssembler resourceAssembler) {
        this.sampleRepository = sampleRepository;
        this.resourceAssembler = resourceAssembler;
    }


    @GetMapping("/{accession}")
    public ResponseEntity<Resource<LegacyGroupsRelations>> getGroupsRelationsByAccession(@PathVariable String accession) {
        Optional<Sample> sample = sampleRepository.findByAccession(accession);
        if(!sample.isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(resourceAssembler.toResource(new LegacyGroupsRelations(sample.get())));
    }

}
