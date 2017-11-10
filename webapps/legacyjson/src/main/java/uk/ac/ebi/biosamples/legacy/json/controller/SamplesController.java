package uk.ac.ebi.biosamples.legacy.json.controller;

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
import uk.ac.ebi.biosamples.legacy.json.domain.LegacySample;
import uk.ac.ebi.biosamples.legacy.json.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;

import java.util.Optional;

@RestController
@RequestMapping(value = "/samples", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(LegacySample.class)
public class SamplesController {

    private final SampleResourceAssembler sampleResourceAssembler;

    private final SampleRepository sampleRepository;

    @Autowired
    public SamplesController(SampleRepository sampleRepository, SampleResourceAssembler sampleResourceAssembler) {

        this.sampleRepository = sampleRepository;
        this.sampleResourceAssembler = sampleResourceAssembler;
    }

    @GetMapping(value = "/{accession}")
    public ResponseEntity<Resource<LegacySample>> sampleByAccession(@PathVariable String accession) {

        Optional<uk.ac.ebi.biosamples.model.Sample> sample = sampleRepository.findByAccession(accession);
        if (!sample.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        LegacySample v3TestSample = new LegacySample(sample.get());
        return ResponseEntity.ok(sampleResourceAssembler.toResource(v3TestSample));

    }

    @GetMapping(value="/")
    public ResponseEntity getIndex() {
        return ResponseEntity.ok(null);
    }






}
