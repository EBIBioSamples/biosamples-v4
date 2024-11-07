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
  private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  private final SampleManipulationService sampleManipulationService;
  private final SampleResourceAssembler sampleResourceAssembler;
  private final PhenopacketConverter phenopacketConverter;
  private final SchemaValidationService schemaValidationService;
  private final TaxonomyClientService taxonomyClientService;
  private final AccessControlService accessControlService;

  public SampleRestController(
      final SampleService sampleService,
      final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
      final SampleManipulationService sampleManipulationService,
      final SampleResourceAssembler sampleResourceAssembler,
      final PhenopacketConverter phenopacketConverter,
      final SchemaValidationService schemaValidationService,
      final TaxonomyClientService taxonomyClientService,
      final AccessControlService accessControlService) {
    this.sampleService = sampleService;
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
      @PathVariable final String accession,
      @RequestParam(name = "legacydetails", required = false) final String legacydetails,
      @RequestParam(name = "curationdomain", required = false) final String[] curationdomain,
      @RequestHeader(name = "Authorization", required = false) final String token) {
    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final Optional<List<String>> decodedCurationDomains =
        LinkUtils.decodeTextsToArray(curationdomain);
    final Optional<Boolean> decodedLegacyDetails =
        "true".equals(legacydetails) ? Optional.of(Boolean.TRUE) : Optional.empty();
    final Optional<Sample> sample = sampleService.fetch(accession, decodedCurationDomains);

    sample.ifPresent(
        s -> {
          bioSamplesWebinAuthenticationService.isSampleAccessible(
              s, authToken.map(AuthToken::getUser).orElse(null));

          decodedLegacyDetails.ifPresent(d -> sampleManipulationService.removeLegacyFields(s));
        });

    return sample
        .map(s -> sampleResourceAssembler.toModel(s, decodedLegacyDetails, decodedCurationDomains))
        .orElseThrow(GlobalExceptions.SampleNotFoundException::new);
  }

  @RequestMapping(produces = "application/phenopacket+json")
  @PreAuthorize("isAuthenticated()")
  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping()
  public String getSamplePhenopacket(
      @PathVariable final String accession,
      @RequestParam(name = "legacydetails", required = false) final String legacydetails,
      @RequestParam(name = "curationdomain", required = false) final String[] curationdomain) {
    final Optional<List<String>> decodedCurationDomains =
        LinkUtils.decodeTextsToArray(curationdomain);
    final Optional<Boolean> decodedLegacyDetails =
        Optional.ofNullable("true".equals(legacydetails) ? Boolean.TRUE : null);
    final Optional<Sample> sample = sampleService.fetch(accession, decodedCurationDomains);

    return sample
        .map(
            s -> {
              bioSamplesWebinAuthenticationService.isSampleAccessible(s, null);
              decodedLegacyDetails.ifPresent(d -> sampleManipulationService.removeLegacyFields(s));

              return phenopacketConverter.convertToJsonPhenopacket(s);
            })
        .orElseThrow(GlobalExceptions.SampleNotFoundException::new);
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public EntityModel<Sample> put(
      @PathVariable final String accession,
      @RequestBody Sample sample,
      @RequestParam(name = "setfulldetails", required = false, defaultValue = "true")
          final boolean setFullDetails,
      @RequestHeader("Authorization") final String token) {
    validateRequest(sample, accession);

    log.debug("Received PUT for " + accession);

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);

    boolean isWebinSuperUser;
    Optional<Sample> oldSample;

    if (authToken.isEmpty()) {
      throw new GlobalExceptions.WebinTokenInvalidException();
    }

    final String webinIdFromAuthToken = authToken.get().getUser();

    if (webinIdFromAuthToken == null) {
      throw new GlobalExceptions.WebinTokenInvalidException();
    }

    isWebinSuperUser = bioSamplesWebinAuthenticationService.isWebinSuperUser(webinIdFromAuthToken);

    if (sampleService.isNotExistingAccession(accession) && !isWebinSuperUser) {
      throw new GlobalExceptions.SampleAccessionDoesNotExistException();
    }

    /*TODO: verify if below is curated or un-curated view, although no technical impact */
    oldSample = sampleService.fetch(sample.getAccession(), Optional.empty());

    sample =
        bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
            sample, webinIdFromAuthToken, oldSample);
    sample = handleAbstractDataForWebinSubmission(sample, webinIdFromAuthToken);

    sample =
        Sample.Builder.fromSample(sample)
            .withUpdate(Instant.now())
            .withSubmittedVia(
                sample.getSubmittedVia() == null
                    ? SubmittedViaType.JSON_API
                    : sample.getSubmittedVia())
            .build();
    sample = validateSample(sample, isWebinSuperUser);

    if (!setFullDetails) {
      log.trace("Removing contact legacy fields for " + accession);

      sample = sampleManipulationService.removeLegacyFields(sample);
    }

    sample =
        sampleService.persistSample(
            sample, oldSample.orElse(null), AuthorizationProvider.WEBIN, isWebinSuperUser);

    // assemble a resource to return
    // create the response object with the appropriate status
    return sampleResourceAssembler.toModel(sample);
  }

  private Sample handleAbstractDataForWebinSubmission(
      Sample sample, final String webinIdFromAuthToken) {
    final SortedSet<AbstractData> abstractData = sample.getData();

    if (abstractData != null && !abstractData.isEmpty()) {
      if (bioSamplesWebinAuthenticationService.isSampleSubmitter(sample, webinIdFromAuthToken)) {
        sample = Sample.Builder.fromSample(sample).build();
      } else {
        sample = Sample.Builder.fromSample(sample).withNoData().build();
      }
    }

    return sample;
  }

  private void validateRequest(final Sample sample, final String accession) {
    if (sample == null) {
      throw new RuntimeException("No sample provided");
    }

    if (sample.getAccession() == null || !sample.getAccession().equals(accession)) {
      throw new GlobalExceptions.SampleAccessionMismatchException();
    }
  }

  private Sample validateSample(Sample sample, final boolean isWebinSuperUser) {
    // Dont validate superuser samples, this helps to submit external (eg. NCBI, ENA) samples
    if (!isWebinSuperUser) {
      schemaValidationService.validate(sample);
      sample = taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(sample, true);
    }

    if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
      schemaValidationService.validate(sample);
    }

    return sample;
  }
}
