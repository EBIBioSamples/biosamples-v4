package uk.ac.ebi.biosamples.legacy.json.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacySample;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.legacy.json.service.PagedResourcesConverter;
import uk.ac.ebi.biosamples.legacy.json.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.model.Sample;

import java.util.Optional;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/samples", produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(LegacySample.class)
public class SamplesController {

    private final SampleRepository sampleRepository;
    private final PagedResourcesConverter pagedResourcesConverter;
    private final SampleResourceAssembler sampleResourceAssembler;

    @Autowired
    public SamplesController(SampleRepository sampleRepository,
                             PagedResourcesConverter pagedResourcesConverter,
                             SampleResourceAssembler sampleResourceAssembler) {

        this.sampleRepository = sampleRepository;
        this.pagedResourcesConverter = pagedResourcesConverter;
        this.sampleResourceAssembler = sampleResourceAssembler;
    }

    @GetMapping(value = "/{accession}")
    public ResponseEntity<Resource<LegacySample>> sampleByAccession(@PathVariable String accession) {

        Optional<Sample> sample = sampleRepository.findByAccession(accession);
        if (!sample.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        LegacySample v3TestSample = new LegacySample(sample.get());
        return ResponseEntity.ok(sampleResourceAssembler.toResource(v3TestSample));

    }

    @GetMapping
    public PagedResources<Resource<LegacySample>> allSamples(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {

        PagedResources<Resource<Sample>> samples = sampleRepository.findSamples(page, size);
        PagedResources<Resource<LegacySample>> pagedResources = pagedResourcesConverter.toLegacySamplesPagedResource(samples);
        pagedResources.add(linkTo(methodOn(SamplesSearchController.class).searchMethods()).withRel("search"));

        return pagedResources;
    }

}
