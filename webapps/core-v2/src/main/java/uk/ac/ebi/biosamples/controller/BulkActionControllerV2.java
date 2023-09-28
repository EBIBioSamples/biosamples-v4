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
import java.util.*;
import java.util.stream.Collectors;
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
@RequestMapping("/samples")
@CrossOrigin
public class BulkActionControllerV2 {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final String SRA_ACCESSION = "SRA accession";
  private final SampleService sampleService;
  private final BioSamplesAapService bioSamplesAapService;
  private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  private final AccessControlService accessControlService;
  private final SchemaValidationService schemaValidationService;
  private final TaxonomyClientService taxonomyClientService;

  public BulkActionControllerV2(
      final SampleService sampleService,
      final BioSamplesAapService bioSamplesAapService,
      final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
      final AccessControlService accessControlService,
      final SchemaValidationService schemaValidationService,
      final TaxonomyClientService taxonomyClientService) {
    this.sampleService = sampleService;
    this.bioSamplesAapService = bioSamplesAapService;
    this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
    this.accessControlService = accessControlService;
    this.schemaValidationService = schemaValidationService;
    this.taxonomyClientService = taxonomyClientService;
  }

  /*
  Bulk accession multiple samples
   */
  @PreAuthorize("isAuthenticated()")
  @PostMapping(
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @RequestMapping("/bulk-accession")
  public ResponseEntity<Map<String, String>> accessionV2(
      @RequestBody List<Sample> samples,
      @RequestHeader(name = "Authorization") final String token) {
    log.info("V2-Received POST for bulk accessioning of " + samples.size() + " samples");

    samples.forEach(
        sample -> {
          if (sample.hasAccession()
              || sample.getAttributes() != null
                  && sample.getAttributes().stream()
                      .anyMatch(attribute -> attribute.getType().equalsIgnoreCase(SRA_ACCESSION))) {
            throw new GlobalExceptions.SampleWithAccessionSubmissionException();
          }
        });

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final boolean webinAuth =
        authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE);

    if (webinAuth) {
      final String webinSubmissionAccountId = authToken.get().getUser();

      if (webinSubmissionAccountId == null) {
        throw new GlobalExceptions.WebinTokenInvalidException();
      }

      samples =
          samples.stream()
              .map(
                  sample ->
                      bioSamplesWebinAuthenticationService.buildSampleWithWebinId(
                          sample, webinSubmissionAccountId))
              .collect(Collectors.toList());
    } else {
      if (!samples.isEmpty()) {
        // check the first sample domain only
        Sample firstSample = samples.get(0);
        firstSample = bioSamplesAapService.handleSampleDomain(firstSample, Optional.empty());

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

                  sample = sampleService.buildPrivateSample(sample);
                  /*
                  Call the accessionSample from SampleService, it doesn't do a lot of housekeeping like reporting to Rabbit,
                  saving to MongoSampleCurated etc which is not required for bulk-accessioning
                   */
                  return sampleService.accessionSample(sample);
                })
            .toList();

    final Map<String, String> outputMap =
        createdSamplesList.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Sample::getName, Sample::getAccession));

    log.info(
        "V2-Received bulk-accessioning request for : "
            + samples.size()
            + " samples and accessioned : "
            + outputMap.size()
            + " samples.");

    return ResponseEntity.ok(outputMap);
  }

  /*
  Bulk fetch multiple samples
   */
  @PreAuthorize("isAuthenticated()")
  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(
      produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE},
      params = "accessions",
      value = "/bulk-fetch")
  public ResponseEntity<Map<String, Sample>> getV2(
      @RequestParam final List<String> accessions,
      @RequestHeader(name = "Authorization", required = false) final String token) {
    if (accessions == null) {
      throw new GlobalExceptions.BulkFetchInvalidRequestException();
    }

    log.info("V2-Received request to bulk-fetch " + accessions.size() + " accessions");

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final List<Sample> samples =
        accessions.stream()
            .map(
                accession -> {
                  final String cleanAccession = accession.trim();
                  final Optional<Sample> sampleOptional =
                      sampleService.fetch(
                          cleanAccession, Optional.of(Collections.singletonList("")));

                  if (sampleOptional.isPresent()) {
                    final boolean webinAuth =
                        authToken
                            .map(t -> t.getAuthority() == AuthorizationProvider.WEBIN)
                            .orElse(Boolean.FALSE);
                    final Sample sample = sampleOptional.get();

                    try {
                      if (webinAuth) {
                        final String webinSubmissionAccountId = authToken.get().getUser();

                        bioSamplesWebinAuthenticationService.isSampleAccessible(
                            sample, webinSubmissionAccountId);
                      } else {
                        bioSamplesAapService.isSampleAccessible(sample);
                      }
                    } catch (final Exception e) {
                      log.info("Bulk-fetch forbidden sample: " + sample.getAccession());

                      return null;
                    }

                    return sample;
                  } else {
                    log.info("Bulk-fetch not found sample: " + accession);

                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .toList();

    log.info(
        "V2-Received bulk-fetch request for : "
            + accessions.size()
            + " samples and fetched : "
            + samples.size()
            + " samples.");

    return ResponseEntity.ok(
        samples.stream()
            .collect(
                Collectors.toMap(
                    sample -> Objects.requireNonNull(sample).getAccession(), sample -> sample)));
  }

  /*
  Submit multiple samples, without any relationship information
   */
  @PreAuthorize("isAuthenticated()")
  @RequestMapping("/bulk-submit")
  @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<List<Sample>> postV2(
      @RequestBody final List<Sample> samples,
      @RequestHeader(name = "Authorization") final String token) {
    log.info("V2-Received POST for " + samples.size() + " samples");

    final List<Sample> createdSamples;
    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final AuthorizationProvider authProvider =
        authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE)
            ? AuthorizationProvider.WEBIN
            : AuthorizationProvider.AAP;

    if (authProvider == AuthorizationProvider.WEBIN) {
      final String webinSubmissionAccountId = authToken.get().getUser();

      if (webinSubmissionAccountId == null) {
        throw new GlobalExceptions.WebinTokenInvalidException();
      }

      createdSamples =
          samples.stream()
              .map(
                  sample ->
                      persistSampleV2WebinAuth(authProvider, webinSubmissionAccountId, sample))
              .collect(Collectors.toList());
    } else {
      createdSamples =
          samples.stream()
              .map(sample -> persistSampleV2AAPAuth(authProvider, sample))
              .collect(Collectors.toList());
    }

    log.info(
        "V2-Received bulk-submit request for : "
            + samples.size()
            + " samples and persisted : "
            + createdSamples.size()
            + " samples.");

    return ResponseEntity.status(HttpStatus.CREATED).body(createdSamples);
  }

  private Sample persistSampleV2AAPAuth(final AuthorizationProvider authProvider, Sample sample) {
    final boolean isAapSuperUser = bioSamplesAapService.isWriteSuperUser();
    final Optional<Sample> oldSample =
        sampleService.validateSampleWithAccessionsAgainstConditionsAndGetOldSample(
            sample, isAapSuperUser);

    sample = bioSamplesAapService.handleSampleDomain(sample, oldSample);
    sample = buildSample(sample, false);

    sampleService.validateSampleHasNoRelationshipsV2(sample);

    if (!isAapSuperUser) {
      sample = validateSample(sample, false);
    }

    return sampleService.persistSampleV2(sample, oldSample.orElse(null), authProvider, false);
  }

  private Sample persistSampleV2WebinAuth(
      final AuthorizationProvider authProvider,
      final String webinSubmissionAccountId,
      Sample sample) {
    final boolean isWebinSuperUser =
        bioSamplesWebinAuthenticationService.isWebinSuperUser(webinSubmissionAccountId);
    final Optional<Sample> oldSample =
        sampleService.validateSampleWithAccessionsAgainstConditionsAndGetOldSample(
            sample, isWebinSuperUser);

    sample =
        bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
            sample, webinSubmissionAccountId, oldSample);
    sample = buildSample(sample, isWebinSuperUser);

    sampleService.validateSampleHasNoRelationshipsV2(sample);

    if (!isWebinSuperUser) {
      sample = validateSample(sample, true);
    }

    return sampleService.persistSampleV2(
        sample, oldSample.orElse(null), authProvider, isWebinSuperUser);
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
}
