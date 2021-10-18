/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exception.SampleNotFoundException;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;
import uk.ac.ebi.biosamples.model.ga4gh.phenopacket.PhenopacketConverter;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.service.SampleManipulationService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.service.taxonomy.ENATaxonClientService;
import uk.ac.ebi.biosamples.utils.LinkUtils;
import uk.ac.ebi.biosamples.validation.SchemaValidationService;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;

/**
 * Primary controller for REST operations both in JSON and XML and both read and write.
 *
 * <p>See {@link SampleHtmlController} for the HTML equivalent controller.
 *
 * @author faulcon
 */
@RestController
@ExposesResourceFor(Sample.class)
@RequestMapping("/samples/{accession}")
@CrossOrigin
public class SampleRestController {
  private Logger log = LoggerFactory.getLogger(getClass());

  private final SampleService sampleService;
  private final BioSamplesAapService bioSamplesAapService;
  private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  private final SampleManipulationService sampleManipulationService;
  private final SampleResourceAssembler sampleResourceAssembler;
  private PhenopacketConverter phenopacketConverter;
  private final SchemaValidationService schemaValidationService;
  private final ENATaxonClientService enaTaxonClientService;

  public SampleRestController(
      SampleService sampleService,
      BioSamplesAapService bioSamplesAapService,
      BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
      SampleManipulationService sampleManipulationService,
      SampleResourceAssembler sampleResourceAssembler,
      PhenopacketConverter phenopacketConverter,
      SchemaValidationService schemaValidationService,
      ENATaxonClientService enaTaxonClientService) {
    this.sampleService = sampleService;
    this.bioSamplesAapService = bioSamplesAapService;
    this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
    this.sampleManipulationService = sampleManipulationService;
    this.sampleResourceAssembler = sampleResourceAssembler;
    this.phenopacketConverter = phenopacketConverter;
    this.schemaValidationService = schemaValidationService;
    this.enaTaxonClientService = enaTaxonClientService;
  }

  @PreAuthorize("isAuthenticated()")
  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public EntityModel<Sample> getSampleHal(
      @PathVariable String accession,
      @RequestParam(name = "legacydetails", required = false) String legacydetails,
      @RequestParam(name = "curationdomain", required = false) String[] curationdomain,
      @RequestParam(name = "curationrepo", required = false) String curationRepo) {

    // decode percent-encoding from curation domains
    Optional<List<String>> decodedCurationDomains = LinkUtils.decodeTextsToArray(curationdomain);
    Optional<Boolean> decodedLegacyDetails;
    if ("true".equals(legacydetails)) {
      decodedLegacyDetails = Optional.of(Boolean.TRUE);
    } else {
      decodedLegacyDetails = Optional.empty();
    }

    // convert it into the format to return
    Optional<Sample> sample = sampleService.fetch(accession, decodedCurationDomains, curationRepo);
    if (sample.isPresent()) {
      bioSamplesAapService.checkAccessible(sample.get());

      // TODO If user is not Read super user, reduce the fields to show
      if (decodedLegacyDetails.isPresent() && decodedLegacyDetails.get()) {
        sample = Optional.of(sampleManipulationService.removeLegacyFields(sample.get()));
      }

      // TODO cache control
      return sampleResourceAssembler.toResource(
          sample.get(), decodedLegacyDetails, decodedCurationDomains);
    } else {
      throw new SampleNotFoundException();
    }
  }

  @RequestMapping(produces = "application/phenopacket+json")
  @PreAuthorize("isAuthenticated()")
  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping()
  public String getSamplePhenopacket(
      @PathVariable String accession,
      @RequestParam(name = "legacydetails", required = false) String legacydetails,
      @RequestParam(name = "curationdomain", required = false) String[] curationdomain,
      @RequestParam(name = "curationrepo", required = false) final String curationRepo) {
    log.trace("starting call");

    // decode percent-encoding from curation domains
    Optional<List<String>> decodedCurationDomains = LinkUtils.decodeTextsToArray(curationdomain);
    Optional<Boolean> decodedLegacyDetails;

    if ("true".equals(legacydetails)) {
      decodedLegacyDetails = Optional.of(Boolean.TRUE);
    } else {
      decodedLegacyDetails = Optional.empty();
    }

    // convert it into the format to return
    Optional<Sample> sample = sampleService.fetch(accession, decodedCurationDomains, curationRepo);

    if (sample.isPresent()) {
      bioSamplesAapService.checkAccessible(sample.get());

      // TODO If user is not Read super user, reduce the fields to show
      if (decodedLegacyDetails.isPresent() && decodedLegacyDetails.get()) {
        sample = Optional.of(sampleManipulationService.removeLegacyFields(sample.get()));
      }

      return phenopacketConverter.convertToJsonPhenopacket(sample.get());
    } else {
      throw new SampleNotFoundException();
    }
  }

  @PreAuthorize("isAuthenticated()")
  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(produces = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
  public Sample getSampleXml(
      @PathVariable String accession,
      @RequestParam(name = "curationrepo", required = false) final String curationRepo) {
    Sample sample = this.getSampleHal(accession, "true", null, curationRepo).getContent();
    if (!sample.getAccession().matches("SAMEG\\d+")) {
      sample =
          Sample.Builder.fromSample(sample)
              .withNoOrganisations()
              .withNoPublications()
              .withNoContacts()
              .build();
    }

    return sample;
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public EntityModel<Sample> put(
      HttpServletRequest request,
      @PathVariable String accession,
      @RequestBody Sample sample,
      @RequestParam(name = "setfulldetails", required = false, defaultValue = "true")
          boolean setFullDetails,
      @RequestParam(name = "authProvider", required = false, defaultValue = "AAP")
          String authProvider) {
    final boolean webinAuth = authProvider.equalsIgnoreCase("WEBIN");
    final SortedSet<AbstractData> abstractData = sample.getData();
    boolean isWebinSuperUser = false;

    if (sample.getAccession() == null || !sample.getAccession().equals(accession)) {
      throw new SampleAccessionMismatchException();
    }

    log.debug("Received PUT for " + accession);

    if (authProvider.equalsIgnoreCase("WEBIN")) {
      final BearerTokenExtractor bearerTokenExtractor = new BearerTokenExtractor();
      final Authentication authentication = bearerTokenExtractor.extract(request);
      final SubmissionAccount webinAccount =
          bioSamplesWebinAuthenticationService
              .getWebinSubmissionAccount(String.valueOf(authentication.getPrincipal()))
              .getBody();

      final String webinAccountId = webinAccount.getId();

      isWebinSuperUser = bioSamplesWebinAuthenticationService.isWebinSuperUser(webinAccountId);

      if (sampleService.isNotExistingAccession(accession) && !isWebinSuperUser) {
        throw new SampleAccessionDoesNotExistException();
      }

      sample = bioSamplesWebinAuthenticationService.handleWebinUser(sample, webinAccountId);

      if (abstractData != null && abstractData.size() > 0) {
        if (bioSamplesWebinAuthenticationService.checkIfOriginalSampleWebinSubmitter(
            sample, webinAccountId)) {
          sample = Sample.Builder.fromSample(sample).build();
        } else {
          sample = Sample.Builder.fromSample(sample).withNoData().build();
        }
      }
    } else {
      if (sampleService.isNotExistingAccession(accession)
          && !(bioSamplesAapService.isWriteSuperUser()
              || bioSamplesAapService.isIntegrationTestUser())) {
        throw new SampleAccessionDoesNotExistException();
      }

      sample = bioSamplesAapService.handleSampleDomain(sample);

      if (abstractData != null && abstractData.size() > 0) {
        if (bioSamplesAapService.checkIfOriginalAAPSubmitter(sample)) {
          sample = Sample.Builder.fromSample(sample).build();
        } else if (bioSamplesAapService.isWriteSuperUser()
            || bioSamplesAapService.isIntegrationTestUser()) {
          sample = Sample.Builder.fromSample(sample).build();
        } else {
          sample = Sample.Builder.fromSample(sample).withNoData().build();
        }
      }
    }

    // now date is system generated field
    Instant now = Instant.now();

    SubmittedViaType submittedVia =
        sample.getSubmittedVia() == null ? SubmittedViaType.JSON_API : sample.getSubmittedVia();

    sample =
        Sample.Builder.fromSample(sample).withUpdate(now).withSubmittedVia(submittedVia).build();

    // Dont validate superuser samples, this helps to submit external (eg. NCBI, ENA) samples
    if (webinAuth && !isWebinSuperUser) {
      schemaValidationService.validate(sample);
    } else if (!webinAuth && !bioSamplesAapService.isWriteSuperUser()) {
      schemaValidationService.validate(sample);
    }

    if (submittedVia == SubmittedViaType.FILE_UPLOADER) {
      schemaValidationService.validate(sample);
    }

    if (webinAuth && !isWebinSuperUser) {
      sample = enaTaxonClientService.performTaxonomyValidation(sample);
    }

    final boolean isFirstTimeMetadataAdded = sampleService.beforeStore(sample, isWebinSuperUser);

    if (isFirstTimeMetadataAdded) {
      sample = Sample.Builder.fromSample(sample).withSubmitted(now).build();
    }

    if (!setFullDetails) {
      log.trace("Removing contact legacy fields for " + accession);
      sample = sampleManipulationService.removeLegacyFields(sample);
    }

    sample = sampleService.store(sample, isFirstTimeMetadataAdded, authProvider);

    // assemble a resource to return
    // create the response object with the appropriate status
    return sampleResourceAssembler.toModel(sample);
  }

  /*At this moment this patching is only for structured data*/
  @PreAuthorize("isAuthenticated()")
  @PatchMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public EntityModel<Sample> patchStructuredData(
      HttpServletRequest request,
      @PathVariable String accession,
      @RequestBody Sample sample,
      @RequestParam(name = "structuredData", required = false, defaultValue = "false")
          boolean structuredData,
      @RequestParam(name = "authProvider", required = false, defaultValue = "AAP")
          String authProvider) {

    if (!structuredData) throw new SampleDataPatchMethodNotSupportedException();

    if (sample.getAccession() == null || !sample.getAccession().equals(accession)) {
      throw new SampleAccessionMismatchException();
    }

    if (authProvider.equalsIgnoreCase("WEBIN")) {
      final BearerTokenExtractor bearerTokenExtractor = new BearerTokenExtractor();
      final Authentication authentication = bearerTokenExtractor.extract(request);
      final SubmissionAccount webinAccount =
          bioSamplesWebinAuthenticationService
              .getWebinSubmissionAccount(String.valueOf(authentication.getPrincipal()))
              .getBody();
      sample =
          bioSamplesWebinAuthenticationService.handleStructuredDataForWebinSubmission(
              sample, webinAccount.getId());
      sample = sampleService.storeSampleStructuredData(sample, authProvider);

      return sampleResourceAssembler.toModel(sample);
    } else {
      if (sampleService.isNotExistingAccession(accession)
          && !(bioSamplesAapService.isWriteSuperUser()
              || bioSamplesAapService.isIntegrationTestUser())) {
        throw new SampleAccessionDoesNotExistException();
      }

      log.debug("Received PATCH for " + accession);

      sample = bioSamplesAapService.handleStructuredDataDomain(sample);
      sample = sampleService.storeSampleStructuredData(sample, authProvider);

      return sampleResourceAssembler.toModel(sample);
    }
  }

  @ResponseStatus(
      value = HttpStatus.BAD_REQUEST,
      reason = "Sample accession must match URL accession") // 400
  public static class SampleAccessionMismatchException extends RuntimeException {}

  @ResponseStatus(
      value = HttpStatus.METHOD_NOT_ALLOWED,
      reason = "Pass argument structuredData=true if you want to PATCH data to sample") // 400
  public static class SampleDataPatchMethodNotSupportedException extends RuntimeException {}

  @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Sample accession does not exist") // 400
  public static class SampleAccessionDoesNotExistException extends RuntimeException {}
}
