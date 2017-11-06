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

@RestController
@RequestMapping(value = "/samplesrelations", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(LegacyRelations.class)
public class LegacyJsonSamplesRelationsController {

    @GetMapping("/{accession}")
    public Resource<LegacyRelations> relationsOfSample(@PathVariable String accession) {
        return new Resource(new LegacyRelations(accession));
    }
}
