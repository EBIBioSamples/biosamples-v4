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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/samples")
@CrossOrigin
public class SamplesRestControllerV2 {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final String SRA_ACCESSION = "SRA accession";
  private final SampleService sampleService;
  private final BioSamplesAapService bioSamplesAapService;
  private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  private final SchemaValidationService schemaValidationService;
  private final TaxonomyClientService taxonomyClientService;
  private final AccessControlService accessControlService;

  public SamplesRestControllerV2(
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
  Submit multiple samples, without any relationship information
   */
  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Sample> postSamplesV2(
      @RequestBody Sample sample, @RequestHeader(name = "Authorization") final String token) {
    log.debug("Received POST for submission " + sample);

    final boolean sampleHasSRAAccessionInAttributes =
        sample.getAttributes() != null
            && sample.getAttributes().stream()
                .anyMatch(attribute -> attribute.getType().equalsIgnoreCase(SRA_ACCESSION));

    if (sample.hasAccession() || sampleHasSRAAccessionInAttributes) {
      throw new GlobalExceptions.SampleWithAccessionSubmissionException();
    }

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final AuthorizationProvider authProvider =
        authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE)
            ? AuthorizationProvider.WEBIN
            : AuthorizationProvider.AAP;
    final boolean isWebinSuperUser;

    if (authProvider == AuthorizationProvider.WEBIN) {
      final String webinSubmissionAccountId = authToken.get().getUser();

      if (webinSubmissionAccountId == null) {
        throw new GlobalExceptions.WebinTokenInvalidException();
      }

      isWebinSuperUser =
          bioSamplesWebinAuthenticationService.isWebinSuperUser(webinSubmissionAccountId);

      sample =
          bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
              sample, webinSubmissionAccountId, Optional.empty());
      sample = buildSample(sample, isWebinSuperUser);

      sampleService.handleSampleRelationshipsV2(sample, Optional.empty(), isWebinSuperUser);

      if (!isWebinSuperUser) {
        sample = validateSample(sample, true);
      }

      sample = sampleService.persistSampleV2(sample, null, authProvider, isWebinSuperUser);
    } else {
      sample = bioSamplesAapService.handleSampleDomain(sample, Optional.empty());
      sample = buildSample(sample, false);

      final boolean isAAPSuperUser = bioSamplesAapService.isWriteSuperUser();

      sampleService.handleSampleRelationshipsV2(sample, Optional.empty(), isAAPSuperUser);

      if (!isAAPSuperUser) {
        sample = validateSample(sample, false);
      }

      sample = sampleService.persistSampleV2(sample, null, authProvider, false);
    }

    return ResponseEntity.status(HttpStatus.CREATED).body(sample);
  }

  private Sample buildSample(final Sample sample, final boolean isWebinSuperUser) {
    return Sample.Builder.fromSample(sample)
        .withCreate(sampleService.defineCreateDate(sample, isWebinSuperUser))
        .withSubmitted(sampleService.defineSubmittedDate(sample, isWebinSuperUser))
        .withUpdate(Instant.now())
        .withSubmittedVia(
            sample.getSubmittedVia() == null ? SubmittedViaType.JSON_API : sample.getSubmittedVia())
        .build();
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
  Accession a single sample
   */
  @PreAuthorize("isAuthenticated()")
  @PostMapping(
      value = "/accession",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Sample> accessionSampleV2(
      @RequestBody Sample sample, @RequestHeader(name = "Authorization") final String token) {
    log.debug("Received POST for accessioning " + sample);

    final boolean sampleHasSRAAccessionInAttributes =
        sample.getAttributes() != null
            && sample.getAttributes().stream()
                .anyMatch(attribute -> attribute.getType().equalsIgnoreCase(SRA_ACCESSION));

    if (sample.hasAccession() || sampleHasSRAAccessionInAttributes) {
      throw new GlobalExceptions.SampleWithAccessionSubmissionException();
    }

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final AuthorizationProvider authProvider =
        authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE)
            ? AuthorizationProvider.WEBIN
            : AuthorizationProvider.AAP;
    boolean isWebinSuperUser = false;

    if (authProvider == AuthorizationProvider.WEBIN) {
      final String webinSubmissionAccountId = authToken.get().getUser();

      if (webinSubmissionAccountId == null) {
        throw new GlobalExceptions.WebinTokenInvalidException();
      }

      isWebinSuperUser =
          bioSamplesWebinAuthenticationService.isWebinSuperUser(webinSubmissionAccountId);

      sample =
          bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
              sample, webinSubmissionAccountId, Optional.empty());
    } else {
      sample = bioSamplesAapService.handleSampleDomain(sample, Optional.empty());
    }

    sample = sampleService.buildPrivateSample(sample);

    sample = sampleService.persistSampleV2(sample, null, authProvider, isWebinSuperUser);

    return ResponseEntity.status(HttpStatus.CREATED).body(sample);
  }
}
