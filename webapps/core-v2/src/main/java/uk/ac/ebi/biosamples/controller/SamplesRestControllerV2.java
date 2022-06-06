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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.ExposesResourceFor;
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
@RequestMapping("/v2/samples")
@CrossOrigin
public class SamplesRestControllerV2 {
  private Logger log = LoggerFactory.getLogger(getClass());

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

  @PreAuthorize("isAuthenticated()")
  @PostMapping(
      value = "/submit",
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<List<Sample>> postSamplesV2(
      @RequestBody final List<Sample> samples,
      @RequestHeader(name = "Authorization") final String token) {
    log.info("Received POST for " + samples.size() + " samples");

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final boolean webinAuth =
        authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE);
    final AuthorizationProvider authProvider =
        webinAuth ? AuthorizationProvider.WEBIN : AuthorizationProvider.AAP;
    boolean isWebinSuperUser;

    if (webinAuth) {
      final String webinSubmissionAccountId = authToken.get().getUser();

      if (webinSubmissionAccountId == null) {
        throw new GlobalExceptions.WebinTokenInvalidException();
      }

      isWebinSuperUser =
          bioSamplesWebinAuthenticationService.isWebinSuperUser(webinSubmissionAccountId);
      final boolean finalIsWebinSuperUser = isWebinSuperUser;

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(
              samples.stream()
                  .map(
                      sample -> {
                        sample =
                            bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
                                sample, webinSubmissionAccountId);

                        if (!finalIsWebinSuperUser) {
                          schemaValidationService.validate(sample);
                          sample =
                              taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(
                                  sample, true);
                        }

                        return sampleService.persistSampleV2(sample, true, authProvider);
                      })
                  .collect(Collectors.toList()));
    } else {
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(
              samples.stream()
                  .map(
                      sample -> {
                        sample = bioSamplesAapService.handleSampleDomain(sample);

                        if (!bioSamplesAapService.isWriteSuperUser()) {
                          schemaValidationService.validate(sample);
                          sample =
                              taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(
                                  sample, false);
                        }

                        return sampleService.persistSampleV2(sample, true, authProvider);
                      })
                  .collect(Collectors.toList()));
    }
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @RequestMapping("/accession")
  public ResponseEntity<Sample> accessionSampleV2(
      @RequestBody Sample sample, @RequestHeader(name = "Authorization") final String token) {

    log.debug("Received POST for accessioning " + sample);
    if (sample.hasAccession()) {
      throw new GlobalExceptions.SampleWithAccessionSubmissionException();
    }

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

      sample =
          bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
              sample, webinSubmissionAccountId);
    } else {
      sample = bioSamplesAapService.handleSampleDomain(sample);
    }

    sample = buildPrivateSampleV2(sample);

    sample = sampleService.persistSampleV2(sample, false, authProvider);

    return ResponseEntity.status(HttpStatus.CREATED).body(sample);
  }

  private Sample buildPrivateSampleV2(final Sample sample) {
    final Instant release =
        Instant.ofEpochSecond(
            LocalDateTime.now(ZoneOffset.UTC).plusYears(100).toEpochSecond(ZoneOffset.UTC));
    final Instant update = Instant.now();
    final SubmittedViaType submittedVia =
        sample.getSubmittedVia() == null ? SubmittedViaType.JSON_API : sample.getSubmittedVia();

    return Sample.Builder.fromSample(sample)
        .withRelease(release)
        .withUpdate(update)
        .withSubmittedVia(submittedVia)
        .build();
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @RequestMapping("/bulk-accession")
  public ResponseEntity<Map<String, String>> bulkAccessionSampleV2(
      @RequestBody List<Sample> samples,
      @RequestHeader(name = "Authorization") final String token) {
    log.info("Received POST for bulk accessioning of " + samples.size() + " samples");

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final boolean webinAuth =
        authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE);

    try {
      samples.forEach(
          sample -> {
            if (sample.hasAccession()) {
              throw new GlobalExceptions.SampleWithAccessionSubmissionException();
            }
          });

      if (webinAuth) {
        final String webinSubmissionAccountId = authToken.get().getUser();

        if (webinSubmissionAccountId == null) {
          throw new GlobalExceptions.WebinTokenInvalidException();
        }

        samples =
            samples.stream()
                .map(
                    sample ->
                        bioSamplesWebinAuthenticationService
                            .buildSampleWithWebinSubmissionAccountId(
                                sample, webinSubmissionAccountId))
                .collect(Collectors.toList());
      } else {
        if (!samples.isEmpty()) {
          Sample firstSample = samples.get(0);
          firstSample = bioSamplesAapService.handleSampleDomain(firstSample);

          final Sample finalFirstSample = firstSample;

          samples =
              samples.stream()
                  .map(
                      sample ->
                          Sample.Builder.fromSample(sample)
                              .withDomain(finalFirstSample.getDomain())
                              .withNoWebinSubmissionAccountId()
                              .build())
                  .collect(Collectors.toList());
        }
      }

      final List<Sample> createdSamplesList =
          samples.stream()
              .map(
                  sample -> {
                    log.trace("Initiating persistSample() for " + sample.getName());
                    sample = buildPrivateSampleV2(sample);
                    return sampleService.accessionSample(sample);
                  })
              .collect(Collectors.toList());

      final Map<String, String> outputMap =
          createdSamplesList.stream()
              .filter(Objects::nonNull)
              .collect(Collectors.toMap(Sample::getName, Sample::getAccession));

      return ResponseEntity.ok(outputMap);
    } catch (final Exception e) {
      log.info("Failed to assign accessions to " + samples.size() + " samples");

      throw new GlobalExceptions.BulkAccessionFailureExceptionV2(e.getMessage());
    }
  }
}
