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
import java.util.Collections;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.AccessControlService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.service.taxonomy.TaxonomyClientService;
import uk.ac.ebi.biosamples.validation.SchemaValidationService;

@RestController
@ExposesResourceFor(Sample.class)
@RequestMapping("/samples/{accession}")
@CrossOrigin
public class SampleRestControllerV2 {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final SampleService sampleService;
  private final BioSamplesAapService bioSamplesAapService;
  private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  private final SchemaValidationService schemaValidationService;
  private final TaxonomyClientService taxonomyClientService;
  private final AccessControlService accessControlService;

  public SampleRestControllerV2(
      final SampleService sampleService,
      final BioSamplesAapService bioSamplesAapService,
      final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
      final SchemaValidationService schemaValidationService,
      final TaxonomyClientService taxonomyClientService,
      final AccessControlService accessControlService) {
    this.sampleService = sampleService;
    this.bioSamplesAapService = bioSamplesAapService;
    this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
    this.schemaValidationService = schemaValidationService;
    this.taxonomyClientService = taxonomyClientService;
    this.accessControlService = accessControlService;
  }

  /*
  Update single sample
   */
  @PreAuthorize("isAuthenticated()")
  @PutMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Sample> putSampleV2(
      @PathVariable final String accession,
      @RequestBody Sample sample,
      @RequestHeader(name = "Authorization") final String token) {
    validateRequest(sample, accession);

    log.debug("Received PUT for " + accession);

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final AuthorizationProvider authProvider =
        authToken
            .filter(t -> t.getAuthority() == AuthorizationProvider.WEBIN)
            .map(t -> AuthorizationProvider.WEBIN)
            .orElse(AuthorizationProvider.AAP);
    boolean isWebinSuperUser = false;
    Optional<Sample> oldSample;

    if (authProvider == AuthorizationProvider.WEBIN) {
      final String webinIdFromAuthToken = authToken.get().getUser();

      if (webinIdFromAuthToken == null) {
        throw new GlobalExceptions.WebinTokenInvalidException();
      }

      isWebinSuperUser =
          bioSamplesWebinAuthenticationService.isWebinSuperUser(webinIdFromAuthToken);

      if (sampleService.isNotExistingAccession(accession) && !isWebinSuperUser) {
        throw new GlobalExceptions.SampleAccessionDoesNotExistException();
      }

      oldSample = sampleService.fetch(sample.getAccession(), Optional.empty());
      sample =
          bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
              sample, webinIdFromAuthToken, oldSample);
    } else {
      if (sampleService.isNotExistingAccession(accession)
          && !(bioSamplesAapService.isWriteSuperUser()
              || bioSamplesAapService.isIntegrationTestUser())) {
        throw new GlobalExceptions.SampleAccessionDoesNotExistException();
      }

      oldSample = sampleService.fetch(sample.getAccession(), Optional.empty());
      sample = bioSamplesAapService.handleSampleDomain(sample, oldSample);
    }

    final Instant now = Instant.now();

    sample =
        Sample.Builder.fromSample(sample)
            .withUpdate(now)
            .withSubmittedVia(
                sample.getSubmittedVia() == null
                    ? SubmittedViaType.JSON_API
                    : sample.getSubmittedVia())
            .build();

    if (!isWebinSuperUser
        || !(bioSamplesAapService.isWriteSuperUser()
            || bioSamplesAapService.isIntegrationTestUser())) {
      sample = validateSample(sample, authProvider == AuthorizationProvider.WEBIN);
    }

    sample =
        sampleService.persistSampleV2(
            sample, oldSample.orElse(null), authProvider, isWebinSuperUser);

    return ResponseEntity.status(HttpStatus.OK).body(sample);
  }

  private void validateRequest(final Sample sample, final String accession) {
    if (sample == null) {
      throw new RuntimeException("No sample provided");
    }

    if (sample.getAccession() == null || !sample.getAccession().equals(accession)) {
      throw new GlobalExceptions.SampleAccessionMismatchException();
    }
  }

  private Sample validateSample(Sample sample, final boolean isWebinSubmission) {
    schemaValidationService.validate(sample);
    sample =
        taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(
            sample, isWebinSubmission);

    if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
      schemaValidationService.validate(sample);
    }

    return sample;
  }

  /*
  Fetch single sample
   */
  @PreAuthorize("isAuthenticated()")
  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public Sample getSampleV2(
      @PathVariable final String accession,
      @RequestHeader(name = "Authorization", required = false) final String token) {
    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final Optional<Sample> optionalSample =
        sampleService.fetch(accession, Optional.of(Collections.singletonList("")));

    if (optionalSample.isPresent()) {
      final AuthorizationProvider authProvider =
          authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE)
              ? AuthorizationProvider.WEBIN
              : AuthorizationProvider.AAP;

      final Sample sample = optionalSample.get();

      if (authProvider == AuthorizationProvider.WEBIN) {
        final String webinSubmissionAccountId = authToken.get().getUser();

        bioSamplesWebinAuthenticationService.isSampleAccessible(sample, webinSubmissionAccountId);
      } else {
        bioSamplesAapService.isSampleAccessible(sample);
      }

      return sample;
    } else {
      throw new GlobalExceptions.SampleNotFoundException();
    }
  }
}
