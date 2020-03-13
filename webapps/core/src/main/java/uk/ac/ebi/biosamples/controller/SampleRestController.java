package uk.ac.ebi.biosamples.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.exception.SampleNotFoundException;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.Ga4ghSampleToPhenopacketConverter;
import uk.ac.ebi.biosamples.service.SampleManipulationService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.utils.LinkUtils;

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
    private Ga4ghSampleToPhenopacketConverter phenopacketExporter;
    private Logger log = LoggerFactory.getLogger(getClass());

    public SampleRestController(SampleService sampleService,
                                BioSamplesAapService bioSamplesAapService,
                                SampleManipulationService sampleManipulationService,
                                SampleResourceAssembler sampleResourceAssembler,
                                Ga4ghSampleToPhenopacketConverter phenopacketExporter,
                                EntityLinks entityLinks) {
        this.sampleService = sampleService;
        this.bioSamplesAapService = bioSamplesAapService;
        this.sampleManipulationService = sampleManipulationService;
        this.sampleResourceAssembler = sampleResourceAssembler;
        this.phenopacketExporter = phenopacketExporter;
        this.entityLinks = entityLinks;
    }

    @PreAuthorize("isAuthenticated()")
    @CrossOrigin(methods = RequestMethod.GET)
    @GetMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public Resource<Sample> getSampleHal(@PathVariable String accession,
                                         @RequestParam(name = "legacydetails", required = false) String legacydetails,
                                         @RequestParam(name = "curationdomain", required = false) String[] curationdomain,
                                         @RequestParam(name = "curationrepo", required = false) String curationRepo) {
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
        Optional<Sample> sample = sampleService.fetch(accession, decodedCurationDomains, curationRepo);
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

    @RequestMapping(produces = "application/phenopacket+json")
    @PreAuthorize("isAuthenticated()")
    @CrossOrigin(methods = RequestMethod.GET)
    @GetMapping()
    public String getSamplePhenopacket(@PathVariable String accession,
                                       @RequestParam(name = "legacydetails", required = false) String legacydetails,
                                       @RequestParam(name = "curationdomain", required = false) String[] curationdomain,
                                       @RequestParam(name = "curationrepo", required = false) final String curationRepo) {
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
        Optional<Sample> sample = sampleService.fetch(accession, decodedCurationDomains, curationRepo);
        if (!sample.isPresent()) {
            throw new SampleNotFoundException();
        }
        bioSamplesAapService.checkAccessible(sample.get());

        // TODO If user is not Read super user, reduce the fields to show
        if (decodedLegacyDetails.isPresent() && decodedLegacyDetails.get()) {
            sample = Optional.of(sampleManipulationService.removeLegacyFields(sample.get()));
        }

        if (!sample.isPresent()) {
            throw new SampleNotFoundException();
        }

        return phenopacketExporter.getJsonFormattedPhenopacketFromSample(sample.get());
    }

    @PreAuthorize("isAuthenticated()")
    @CrossOrigin(methods = RequestMethod.GET)
    @GetMapping(produces = { MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE })
    public Sample getSampleXml(@PathVariable String accession,
                               @RequestParam(name = "curationrepo", required = false) final String curationRepo) {
            Sample sample = this.getSampleHal(accession, "true", null, curationRepo).getContent();
            if (!sample.getAccession().matches("SAMEG\\d+")) {
//			sample = Sample.build(sample.getName(),sample.getAccession(), sample.getDomain(),
//					sample.getRelease(), sample.getUpdate(), sample.getCharacteristics(), sample.getRelationships(),
//					sample.getExternalReferences(), null, null, null);
        sample = Sample.Builder.fromSample(sample)
                                    .withNoOrganisations().withNoPublications().withNoContacts()
                                    .build();
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


    @PreAuthorize("isAuthenticated()")
    @PutMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
    public Resource<Sample> put(@PathVariable String accession,
                                @RequestBody Sample sample,
                                @RequestParam(name = "setupdatedate", required = false, defaultValue = "true") boolean setUpdateDate,
                                @RequestParam(name = "setfulldetails", required = false, defaultValue = "true") boolean setFullDetails,
                                @RequestParam(name = "submitStructuredData", required = false, defaultValue = "false") boolean submitStructuredDta) {

        if (sample.getAccession() == null || !sample.getAccession().equals(accession)) {
            // if the accession in the body is different to the accession in the
            // datasetUrl, throw an error
            // TODO create proper exception with right http error code
            throw new SampleAccessionMismatchException();
        }

        // todo Fix all integration tests to not to use predefined accessions, then remove isIntegrationTestUser() check
        if (!sampleService.isExistingAccession(accession) && !(bioSamplesAapService.isWriteSuperUser() || bioSamplesAapService.isIntegrationTestUser())) {
            throw new SampleAccessionDoesNotExistException();
        }

        log.debug("Recieved PUT for " + accession);

        if (submitStructuredDta) {
            sample = sampleService.storeSampleStructuredData(sample);

            return sampleResourceAssembler.toResource(sample);
        } else {
            sample = bioSamplesAapService.handleSampleDomain(sample);

            //update date is system generated field
            Instant update = Instant.now();
            SubmittedViaType submittedVia =
                    sample.getSubmittedVia() == null ? SubmittedViaType.JSON_API : sample.getSubmittedVia();
            sample = Sample.Builder.fromSample(sample)
                    .withUpdate(update)
                    .withSubmittedVia(submittedVia).build();

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

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Sample accession must match URL accession") // 400
    public static class SampleAccessionMismatchException extends RuntimeException {
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Sample accession does not exist") // 400
    public static class SampleAccessionDoesNotExistException extends RuntimeException {
    }
}
