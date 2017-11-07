package uk.ac.ebi.biosamples.api;

import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.model.LegacyRelations;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.LegacyRelationsResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;

@RestController
@RequestMapping(value = "/samplesrelations", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(LegacyRelations.class)
public class LegacyJsonSamplesRelationsController {

    private final SampleService sampleService;
    private final LegacyRelationsResourceAssembler relationsResourceAssembler;

    public LegacyJsonSamplesRelationsController(SampleService sampleService, LegacyRelationsResourceAssembler relationsResourceAssembler) {
        this.sampleService = sampleService;
        this.relationsResourceAssembler = relationsResourceAssembler;
    }

    @GetMapping("/{accession}")
    public Resource<LegacyRelations> relationsOfSample(@PathVariable String accession) {
        Sample sample = sampleService.findByAccession(accession);
        return relationsResourceAssembler.toResource(new LegacyRelations(sample));
    }

    @GetMapping("/{accession}/groups")
    public Object getSamplesGroupRelations(@PathVariable String accession) {
        return "{\"_embedded\": {\"groupsrelations\": [{\"accession\": \"SAMEG222\"}]},\"_links\": {\"self\": {\"href\":\"test\"}}}";
    }

}
