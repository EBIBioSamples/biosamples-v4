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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uk.ac.ebi.biosamples.authentication.WebinAuthenticationService;
import uk.ac.ebi.biosamples.core.model.Relationship;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.SubmissionReceipt;
import uk.ac.ebi.biosamples.core.model.SubmittedViaType;
import uk.ac.ebi.biosamples.core.service.SampleService;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;
import uk.ac.ebi.biosamples.properties.BioSamplesProperties;
import uk.ac.ebi.biosamples.service.validation.SchemaValidationService;

@RestController
@ExposesResourceFor(Sample.class)
@RequestMapping("/samples")
@CrossOrigin
public class BulkActionControllerV2 {
  private static final String SRA_ACCESSION = "SRA accession";
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final SampleService sampleService;
  private final WebinAuthenticationService webinAuthenticationService;
  private final SchemaValidationService schemaValidationService;
  private final BioSamplesProperties bioSamplesProperties;
  private final ObjectMapper objectMapper;

  public BulkActionControllerV2(
      final SampleService sampleService,
      final WebinAuthenticationService webinAuthenticationService,
      final SchemaValidationService schemaValidationService,
      final BioSamplesProperties bioSamplesProperties,
      final ObjectMapper objectMapper) {
    this.sampleService = sampleService;
    this.webinAuthenticationService = webinAuthenticationService;
    this.schemaValidationService = schemaValidationService;
    this.bioSamplesProperties = bioSamplesProperties;
    this.objectMapper = objectMapper;
  }

  /*
  Bulk accession multiple samples
   */
  @PreAuthorize("isAuthenticated()")
  @PostMapping(
      value = "/bulk-accession",
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Map<String, String>> accessionV2(@RequestBody List<Sample> samples) {
    log.info("V2-Received POST for bulk accessioning called");

    final var loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final var principle = sampleService.getPrinciple(loggedInUser);

    if (principle == null) {
      throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
    }

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

    samples =
        samples.stream()
            .map(sample -> webinAuthenticationService.buildSampleWithWebinId(sample, principle))
            .collect(Collectors.toList());

    final var createdSamplesList =
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

    final var outputMap =
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
      value = "/bulk-fetch",
      produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE},
      params = "accessions")
  public ResponseEntity<Map<String, Sample>> getV2(
      @RequestParam final List<String> accessions,
      @RequestHeader(name = "Authorization", required = false) final String token) {
    final var loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final var principle = sampleService.getPrinciple(loggedInUser);

    if (accessions == null) {
      throw new GlobalExceptions.BulkFetchInvalidRequestException();
    }

    log.info("V2-Received request to bulk-fetch " + accessions.size() + " accessions");

    final var samples =
        accessions.stream()
            .map(
                accession -> {
                  final var accesionWithoutSpaces = accession.trim();
                  final var sampleOptional =
                      // fetch returns sample with no-curations applied
                      sampleService.fetch(accesionWithoutSpaces, false);

                  if (sampleOptional.isPresent()) {
                    final var sample = sampleOptional.get();

                    try {
                      webinAuthenticationService.isSampleAccessible(sample, principle);
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
                    sample -> Objects.requireNonNull(sample).getAccession(), Function.identity())));
  }

  /*
  Validate multiple samples, without any relationship information
   */
  @PreAuthorize("isAuthenticated()")
  @PostMapping(
      value = "/bulk-submit-get-receipt",
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<SubmissionReceipt> postV2(@RequestBody final List<Sample> samples) {
    final var loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final var principle = sampleService.getPrinciple(loggedInUser);

    if (principle == null) {
      throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
    }

    log.info("V2-Received POST with validation for {} samples", samples.size());

    final var createdSamples = new ArrayList<Sample>();
    final var errors = new ArrayList<SubmissionReceipt.ErrorReceipt>();

    for (final var sample : samples) {
      final var sampleErrorPair = persistSample(principle, sample);

      sampleErrorPair.getLeft().ifPresent(createdSamples::add);
      sampleErrorPair
          .getRight()
          .ifPresent(
              err -> {
                List<SubmissionReceipt.ValidationError> validationErrors;

                try {
                  validationErrors = objectMapper.readValue(err, new TypeReference<>() {});
                } catch (JsonProcessingException e) {
                  validationErrors =
                      Collections.singletonList(
                          new SubmissionReceipt.ValidationError(
                              "", Collections.singletonList(err)));
                }

                errors.add(new SubmissionReceipt.ErrorReceipt(sample.getName(), validationErrors));
              });
    }

    log.info(
        "V2-Received bulk-submit-get-receipt request for : "
            + samples.size()
            + " samples and persisted : "
            + createdSamples.size()
            + " samples.");

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new SubmissionReceipt(createdSamples, errors));
  }

  /*
  Submit multiple samples, without any relationship information
   */
  @PreAuthorize("isAuthenticated()")
  @RequestMapping("/bulk-submit")
  @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<List<Sample>> postV2NoValidation(@RequestBody final List<Sample> samples) {
    log.info("V2-Received POST for {} samples", samples.size());

    final var loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final var principle = sampleService.getPrinciple(loggedInUser);

    if (principle == null) {
      throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
    }

    if (!webinAuthenticationService.isWebinSuperUser(principle)) {
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "POST for super users only");
    }

    final var createdSamples =
        samples.stream()
            .map(sample -> persistSampleNoValidation(principle, sample))
            .collect(Collectors.toList());

    log.info(
        "V2-Received bulk-submit request for : {} samples and persisted : {} samples.",
        samples.size(),
        createdSamples.size());

    return ResponseEntity.status(HttpStatus.CREATED).body(createdSamples);
  }

  /*
  Submit multiple samples, without any relationship information
   */
  @PostMapping(
      value = "/bulk-validate",
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<SubmissionReceipt> validateV2(@RequestBody final List<Sample> samples) {
    final var loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final var principle = sampleService.getPrinciple(loggedInUser);

    if (principle == null) {
      throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
    }

    log.info("V2-Received Validate request for {} samples", samples.size());

    final var errors = new ArrayList<SubmissionReceipt.ErrorReceipt>();

    var validationErrors = new ArrayList<SubmissionReceipt.ValidationError>();

    for (final Sample sample : samples) {
      final String validationResult = validateGetMessages(sample, principle);

      if (validationResult != null) {
        try {
          validationErrors = objectMapper.readValue(validationResult, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
          validationErrors =
              new ArrayList<>(
                  Collections.singletonList(
                      new SubmissionReceipt.ValidationError(
                          "", Collections.singletonList(validationResult))));
        }

        errors.add(new SubmissionReceipt.ErrorReceipt(sample.getName(), validationErrors));
      }
    }

    log.info(
        "V2-Received bulk-validate request for : {} samples and validated : {} samples.",
        samples.size(),
        samples.size());

    return ResponseEntity.status(HttpStatus.OK).body(new SubmissionReceipt(null, errors));
  }

  private Pair<Optional<Sample>, Optional<String>> persistSample(
      final String principle, Sample sample) {
    final var isWebinSuperUser = webinAuthenticationService.isWebinSuperUser(principle);
    final var oldSample =
        sampleService.validateSampleWithAccessionsAgainstConditionsAndGetOldSample(
            sample, isWebinSuperUser);
    final var relationships =
        sampleService.handleSampleRelationshipsV2(sample, oldSample, isWebinSuperUser);

    sample = webinAuthenticationService.handleWebinUserSubmission(sample, principle, oldSample);

    sample = buildSample(sample, relationships, isWebinSuperUser);

    Pair<Optional<Sample>, Optional<String>> sampleErrorPair;

    try {
      if (bioSamplesProperties.isEnableBulkSubmissionWebinSuperUserValidation()
          || !isWebinSuperUser) {
        validate(sample, principle);
      }

      final Optional<Sample> persistedSample =
          Optional.of(
              sampleService.persistSampleV2(sample, oldSample.orElse(null), isWebinSuperUser));
      sampleErrorPair = new ImmutablePair<>(persistedSample, Optional.empty());
    } catch (GlobalExceptions.SchemaValidationException e) {
      sampleErrorPair = new ImmutablePair<>(Optional.empty(), Optional.ofNullable(e.getMessage()));

      final var accession = sample.getAccession();

      log.info("Sample validation failed: {}", accession != null ? accession : sample.getName());
    } catch (Exception e) {
      sampleErrorPair = new ImmutablePair<>(Optional.empty(), Optional.ofNullable(e.getMessage()));

      log.error("Failed to validate sample", e);
    }

    return sampleErrorPair;
  }

  private Sample persistSampleNoValidation(final String principle, Sample sample) {
    final var oldSample =
        sampleService.validateSampleWithAccessionsAgainstConditionsAndGetOldSample(sample, true);
    final var relationships = sampleService.handleSampleRelationshipsV2(sample, oldSample, true);

    sample = webinAuthenticationService.handleWebinUserSubmission(sample, principle, oldSample);
    sample = buildSample(sample, relationships, true);

    return sampleService.persistSampleV2(sample, oldSample.orElse(null), true);
  }

  private String validateGetMessages(final Sample sample, final String principle) {
    final var sampleIdentifier =
        sample.getAccession() != null ? sample.getAccession() : sample.getName();
    try {
      schemaValidationService.validate(sample, principle);
    } catch (GlobalExceptions.SchemaValidationException e) {
      log.info("Sample validation has failed: {}", sample.getAccession());

      return Optional.ofNullable(e.getMessage())
          .orElse("Unknown validation error while validating sample: " + sampleIdentifier);
    } catch (Exception e) {
      log.error("Failed to validate sample", e);

      return Optional.ofNullable(e.getMessage())
          .orElse("Unknown validation error while validating sample: " + sampleIdentifier);
    }

    return null;
  }

  private void validate(final Sample sample, final String principle) {
    schemaValidationService.validate(sample, principle);
  }

  private Sample buildSample(
      final Sample sample, final Set<Relationship> relationships, final boolean isWebinSuperUser) {
    return Sample.Builder.fromSample(sample)
        .withRelationships(relationships)
        .withCreate(sampleService.defineCreateDate(sample, isWebinSuperUser))
        .withSubmitted(sampleService.defineSubmittedDate(sample, isWebinSuperUser))
        .withUpdate(Instant.now())
        .withSubmittedVia(
            sample.getSubmittedVia() == null ? SubmittedViaType.JSON_API : sample.getSubmittedVia())
        .build();
  }
}
