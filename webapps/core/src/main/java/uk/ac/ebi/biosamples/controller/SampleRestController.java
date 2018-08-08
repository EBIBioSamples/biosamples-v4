package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exception.SampleNotFoundException;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.*;
import uk.ac.ebi.biosamples.utils.LinkUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Primary controller for REST operations both in JSON and XML and both read and
 * write.
 *
 * See {@link SampleHtmlController} for the HTML equivalent controller.
 *
 * @author faulcon
 */
@RestController
@ExposesResourceFor(Sample.class)
@RequestMapping("/samples/{accession}")
public class SampleRestController {

    private final SampleService sampleService;
    private final BioSamplesAapService bioSamplesAapService;
    private final SampleManipulationService sampleManipulationService;

    private final SampleResourceAssembler sampleResourceAssembler;

    private final EntityLinks entityLinks;


    private Logger log = LoggerFactory.getLogger(getClass());

    public SampleRestController(SampleService sampleService,
                                BioSamplesAapService bioSamplesAapService,
                                SampleManipulationService sampleManipulationService,
                                SampleResourceAssembler sampleResourceAssembler,
                                EntityLinks entityLinks) {
        this.sampleService = sampleService;
        this.bioSamplesAapService = bioSamplesAapService;
        this.sampleManipulationService = sampleManipulationService;
        this.sampleResourceAssembler = sampleResourceAssembler;
        this.entityLinks = entityLinks;
    }

    @PreAuthorize("isAuthenticated()")
    @CrossOrigin(methods = RequestMethod.GET)
    @GetMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public Resource<Sample> getSampleHal(@PathVariable String accession,
                                         @RequestParam(name = "legacydetails", required = false) String legacydetails,
                                         @RequestParam(name = "curationdomain", required = false) String[] curationdomain) {
        log.trace("starting call");

        // decode percent-encoding from curation domains
        Optional<List<String>> decodedCurationDomains = LinkUtils.decodeTextsToArray(curationdomain);
        Optional<Boolean> decodedLegacyDetails;
        if (legacydetails != null && "true".equals(legacydetails)) {
            decodedLegacyDetails = Optional.ofNullable(Boolean.TRUE);
        } else {
            decodedLegacyDetails = Optional.empty();
        }

        // convert it into the format to return
        Optional<Sample> sample = sampleService.fetch(accession, decodedCurationDomains);
        if (!sample.isPresent()) {
            throw new SampleNotFoundException();
        }
        bioSamplesAapService.checkAccessible(sample.get());

        // TODO If user is not Read super user, reduce the fields to show
        if (decodedLegacyDetails.isPresent() && decodedLegacyDetails.get()) {
            sample = Optional.of(sampleManipulationService.removeLegacyFields(sample.get()));
        }

        Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample.get(),
                decodedLegacyDetails, decodedCurationDomains);

        //TODO cache control
        return sampleResource;
    }


    @PreAuthorize("isAuthenticated()")
    @CrossOrigin(methods = RequestMethod.GET)
    @GetMapping(produces = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public Sample getSampleXml(@PathVariable String accession) {
        Sample sample = this.getSampleHal(accession, "true", null).getContent();
        if (!sample.getAccession().matches("SAMEG\\d+")) {
            sample = Sample.build(sample.getName(), sample.getAccession(), sample.getDomain(),
                    sample.getRelease(), sample.getUpdate(), sample.getCharacteristics(), sample.getRelationships(),
                    sample.getExternalReferences(), null, null, null);
        }
        //TODO cache control
        return sample;
    }

//    @PreAuthorize("isAuthenticated()")
//	@CrossOrigin(methods = RequestMethod.GET)
//    @GetMapping(produces = "application/ld+json")
//    public JsonLDRecord getJsonLDSample(@PathVariable String accession) {
//		Optional<Sample> sample = sampleService.fetch(accession);
//		if (!sample.isPresent()) {
//			throw new SampleNotFoundException();
//		}
//		bioSamplesAapService.checkAccessible(sample.get());
//
//        // check if the release date is in the future and if so return it as
//        // private
//        if (sample.get().getRelease().isAfter(Instant.now())) {
//			throw new SampleNotAccessibleException();
//        }
//
//		return jsonLDService.sampleToJsonLD(sample.get());
//    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Sample accession must match URL accession") // 400
    public static class SampleAccessionMismatchException extends RuntimeException {
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
    public Resource<Sample> put(@PathVariable String accession,
                                @RequestBody Sample sample,
                                @RequestParam(name = "setupdatedate", required = false, defaultValue = "true") boolean setUpdateDate,
                                @RequestParam(name = "setfulldetails", required = false, defaultValue = "false") boolean setFullDetails) {

        if (sample.getAccession() == null || !sample.getAccession().equals(accession)) {
            // if the accession in the body is different to the accession in the
            // datasetUrl, throw an error
            // TODO create proper exception with right http error code
            throw new SampleAccessionMismatchException();
        }

        log.debug("Recieved PUT for " + accession);
        sample = bioSamplesAapService.handleSampleDomain(sample);

        //TODO limit use of this method to write super-users only
        //if (bioSamplesAapService.isWriteSuperUser() && setUpdateDate) {
        if (setUpdateDate) {
            sample = Sample.build(sample.getName(), sample.getAccession(), sample.getDomain(),
                    sample.getRelease(), Instant.now(),
                    sample.getCharacteristics(), sample.getRelationships(), sample.getExternalReferences(),
                    sample.getOrganizations(), sample.getContacts(), sample.getPublications());
        }

        if (!setFullDetails) {
            log.trace("Removing contact legacy fields for " + accession);
            sample = sampleManipulationService.removeLegacyFields(sample);
        }

        sample = sampleService.store(sample);

        // assemble a resource to return
        Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);

        // create the response object with the appropriate status
        return sampleResource;
    }


}
