package uk.ac.ebi.biosamples.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exception.SampleNotAccessibleException;
import uk.ac.ebi.biosamples.exception.SampleNotFoundException;
import uk.ac.ebi.biosamples.model.JsonLDDataCatalog;
import uk.ac.ebi.biosamples.model.JsonLDDataset;
import uk.ac.ebi.biosamples.model.JsonLDDataRecord;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.JsonLDService;
import uk.ac.ebi.biosamples.service.SampleService;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping(produces="application/ld+json")
public class BioschemasController {

    private final JsonLDService jsonLDService;
    private final SampleService sampleService;
    private final BioSamplesAapService bioSamplesAapService;

    public BioschemasController(JsonLDService service, SampleService sampleService, BioSamplesAapService bioSamplesAapService) {
        this.jsonLDService = service;
        this.sampleService = sampleService;
        this.bioSamplesAapService = bioSamplesAapService;
    }

    @CrossOrigin(methods = RequestMethod.GET)
    @GetMapping(value="/")
    public JsonLDDataCatalog rootBioschemas() {
        return jsonLDService.getBioSamplesDataCatalog();
    }

    @CrossOrigin(methods = RequestMethod.GET)
    @GetMapping(value="/samples")
    public JsonLDDataset biosamplesDataset() {
        return jsonLDService.getBioSamplesDataset();
    }


    @PreAuthorize("isAuthenticated()")
    @CrossOrigin(methods = RequestMethod.GET)
    @GetMapping(value="/samples/{accession}", produces = "application/ld+json")
    public JsonLDDataRecord getJsonLDSample(@PathVariable String accession,
                                            @RequestParam(name = "curationrepo", required = false) final String curationRepo) {
        Optional<Sample> sample = sampleService.fetch(accession, Optional.empty(), curationRepo);
        if (!sample.isPresent()) {
            throw new SampleNotFoundException();
        }
        bioSamplesAapService.checkAccessible(sample.get());

        // check if the release date is in the future and if so return it as
        // private
        if (sample.get().getRelease().isAfter(Instant.now())) {
            throw new SampleNotAccessibleException();
        }

        return jsonLDService.sampleToJsonLD(sample.get());
    }

}
