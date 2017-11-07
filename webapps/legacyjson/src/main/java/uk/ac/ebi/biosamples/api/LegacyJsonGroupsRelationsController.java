package uk.ac.ebi.biosamples.api;

import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.model.LegacyGroupsRelations;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.LegacyGroupsRelationsResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;

@RestController
@RequestMapping(value = "/groupsrelations", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(LegacyGroupsRelations.class)
public class LegacyJsonGroupsRelationsController {

    private final SampleService sampleService;
    private final LegacyGroupsRelationsResourceAssembler resourceAssembler;

    public LegacyJsonGroupsRelationsController(SampleService sampleService, LegacyGroupsRelationsResourceAssembler resourceAssembler) {
        this.sampleService = sampleService;
        this.resourceAssembler = resourceAssembler;
    }


    @GetMapping("/{accession}")
    public Resource<LegacyGroupsRelations> getGroupsRelationsByAccession(@PathVariable String accession) {
        Sample sample = sampleService.findByAccession(accession);
        return resourceAssembler.toResource(new LegacyGroupsRelations(sample));
    }

}
