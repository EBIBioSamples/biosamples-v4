package uk.ac.ebi.biosamples.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.model.LegacySample;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.LegacySampleResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;

@RestController
@RequestMapping(value = "/samples", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(LegacySample.class)
public class LegacyJsonSamplesController {

    private final LegacySampleResourceAssembler sampleResourceAssembler;

    private final SampleService sampleService;

    @Autowired
    public LegacyJsonSamplesController(SampleService sampleService, LegacySampleResourceAssembler sampleResourceAssembler) {

        this.sampleService = sampleService;
        this.sampleResourceAssembler = sampleResourceAssembler;
    }

    @GetMapping(value = "/{accession}")
    public Resource<LegacySample> sampleByAccession(@PathVariable String accession) {

        Sample testSample = sampleService.findByAccession(accession);
        LegacySample v3TestSample = new LegacySample(testSample);

        return sampleResourceAssembler.toResource(v3TestSample);

    }

    @GetMapping(value="/")
    public ResponseEntity getIndex() {
        return ResponseEntity.ok(null);
    }






}
