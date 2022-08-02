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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.service.SampleManipulationService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.AccessControlService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.service.taxonomy.TaxonomyClientService;
import uk.ac.ebi.biosamples.utils.LinkUtils;
import uk.ac.ebi.biosamples.utils.phenopacket.PhenopacketConverter;
import uk.ac.ebi.biosamples.validation.SchemaValidationService;

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
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final SampleService sampleService;
  private final BioSamplesAapService bioSamplesAapService;
  private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  private final SampleManipulationService sampleManipulationService;
  private final SampleResourceAssembler sampleResourceAssembler;
  private final PhenopacketConverter phenopacketConverter;
  private final SchemaValidationService schemaValidationService;
  private final TaxonomyClientService taxonomyClientService;
  private final AccessControlService accessControlService;

  public SampleRestController(
      SampleService sampleService,
      BioSamplesAapService bioSamplesAapService,
      BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
      SampleManipulationService sampleManipulationService,
      SampleResourceAssembler sampleResourceAssembler,
      PhenopacketConverter phenopacketConverter,
      SchemaValidationService schemaValidationService,
      TaxonomyClientService taxonomyClientService,
      AccessControlService accessControlService) {
    this.sampleService = sampleService;
    this.bioSamplesAapService = bioSamplesAapService;
    this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
    this.sampleManipulationService = sampleManipulationService;
    this.sampleResourceAssembler = sampleResourceAssembler;
    this.phenopacketConverter = phenopacketConverter;
    this.schemaValidationService = schemaValidationService;
    this.taxonomyClientService = taxonomyClientService;
    this.accessControlService = accessControlService;
  }

  @PreAuthorize("isAuthenticated()")
  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public EntityModel<Sample> getSampleHal(
      @PathVariable String accession,
      @RequestParam(name = "legacydetails", required = false) String legacydetails,
      @RequestParam(name = "curationdomain", required = false) String[] curationdomain,
      @RequestParam(name = "curationrepo", required = false) String curationRepo,
      @RequestHeader(name = "Authorization", required = false) final String token) {
    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    // decode percent-encoding from curation domains
    final Optional<List<String>> decodedCurationDomains =
        LinkUtils.decodeTextsToArray(curationdomain);
    final Optional<Boolean> decodedLegacyDetails;

    if ("true".equals(legacydetails)) {
      decodedLegacyDetails = Optional.of(Boolean.TRUE);
    } else {
      decodedLegacyDetails = Optional.empty();
    }

    // convert it into the format to return
    Optional<Sample> sample = sampleService.fetch(accession, decodedCurationDomains, curationRepo);

    if (sample.isPresent()) {
      final boolean webinAuth =
          authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE);

      if (webinAuth) {
        final String webinSubmissionAccountId = authToken.get().getUser();

        bioSamplesWebinAuthenticationService.isSampleAccessible(
            sample.get(), webinSubmissionAccountId);
      } else {
        bioSamplesAapService.checkSampleAccessibility(sample.get());
      }

      if (decodedLegacyDetails.isPresent() && decodedLegacyDetails.get()) {
        sample = Optional.of(sampleManipulationService.removeLegacyFields(sample.get()));
      }

      return sampleResourceAssembler.toModel(
          sample.get(), decodedLegacyDetails, decodedCurationDomains);
    } else {
      throw new GlobalExceptions.SampleNotFoundException();
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
      bioSamplesAapService.checkSampleAccessibility(sample.get());

      // TODO If user is not Read super user, reduce the fields to show
      if (decodedLegacyDetails.isPresent() && decodedLegacyDetails.get()) {
        sample = Optional.of(sampleManipulationService.removeLegacyFields(sample.get()));
      }

      return phenopacketConverter.convertToJsonPhenopacket(sample.get());
    } else {
      throw new GlobalExceptions.SampleNotFoundException();
    }
  }

  @PreAuthorize("isAuthenticated()")
  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(produces = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
  public Sample getSampleXml(
      @PathVariable String accession,
      @RequestParam(name = "curationrepo", required = false) final String curationRepo,
      @RequestHeader(name = "Authorization", required = false) final String token) {
    Sample sample = getSampleHal(accession, "true", null, curationRepo, token).getContent();
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
      @PathVariable String accession,
      @RequestBody Sample sample,
      @RequestParam(name = "setfulldetails", required = false, defaultValue = "true")
          boolean setFullDetails,
      @RequestHeader("Authorization") final String token) {

    if (sample == null) {
      throw new RuntimeException("No sample provided");
    }
    final SortedSet<AbstractData> abstractData = sample.getData();
    boolean isWebinSuperUser = false;

    if (sample.getAccession() == null || !sample.getAccession().equals(accession)) {
      throw new GlobalExceptions.SampleAccessionMismatchException();
    }

    log.debug("Received PUT for " + accession);

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final boolean webinAuth =
        authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE);
    final AuthorizationProvider authProvider =
        webinAuth ? AuthorizationProvider.WEBIN : AuthorizationProvider.AAP;

    if (webinAuth) {
      final String webinSubmissionAccountId = authToken.get().getUser();

      if (webinSubmissionAccountId == null) {
        throw new GlobalExceptions.WebinTokenInvalidException();
      }

      isWebinSuperUser =
          bioSamplesWebinAuthenticationService.isWebinSuperUser(webinSubmissionAccountId);

      if (sampleService.isNotExistingAccession(accession) && !isWebinSuperUser) {
        throw new GlobalExceptions.SampleAccessionDoesNotExistException();
      }

      sample =
          bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
              sample, webinSubmissionAccountId);

      if (abstractData != null && abstractData.size() > 0) {
        if (bioSamplesWebinAuthenticationService.isSampleSubmitter(
            sample, webinSubmissionAccountId)) {
          sample = Sample.Builder.fromSample(sample).build();
        } else {
          sample = Sample.Builder.fromSample(sample).withNoData().build();
        }
      }
    } else {
      if (sampleService.isNotExistingAccession(accession)
          && !(bioSamplesAapService.isWriteSuperUser()
              || bioSamplesAapService.isIntegrationTestUser())) {
        throw new GlobalExceptions.SampleAccessionDoesNotExistException();
      }

      sample = bioSamplesAapService.handleSampleDomain(sample);

      if (abstractData != null && abstractData.size() > 0) {
        if (bioSamplesAapService.isStructuredDataSubmittedBySampleSubmitter(sample)) {
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
    final Instant now = Instant.now();

    SubmittedViaType submittedVia =
        sample.getSubmittedVia() == null ? SubmittedViaType.JSON_API : sample.getSubmittedVia();

    sample =
        Sample.Builder.fromSample(sample).withUpdate(now).withSubmittedVia(submittedVia).build();

    sample = validateSample(sample, webinAuth, isWebinSuperUser);

    if (!setFullDetails) {
      log.trace("Removing contact legacy fields for " + accession);
      sample = sampleManipulationService.removeLegacyFields(sample);
    }

    sample = sampleService.persistSample(sample, authProvider, isWebinSuperUser);

    // assemble a resource to return
    // create the response object with the appropriate status
    return sampleResourceAssembler.toModel(sample);
  }

  private Sample validateSample(Sample sample, boolean webinAuth, boolean isWebinSuperUser) {
    // Dont validate superuser samples, this helps to submit external (eg. NCBI, ENA) samples
    if (webinAuth && !isWebinSuperUser) {
      schemaValidationService.validate(sample);
      sample = taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(sample, true);
    } else if (!webinAuth && !bioSamplesAapService.isWriteSuperUser()) {
      schemaValidationService.validate(sample);
      sample = taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(sample, false);
    }

    if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
      schemaValidationService.validate(sample);
    }

    return sample;
  }
}
