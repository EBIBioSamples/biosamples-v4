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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.WebinAuthenticationService;
import uk.ac.ebi.biosamples.validation.SchemaValidationService;

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

    final Authentication loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final String principle = sampleService.getPrinciple(loggedInUser);

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
      value = "/bulk-fetch",
      produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE},
      params = "accessions")
  public ResponseEntity<Map<String, Sample>> getV2(
      @RequestParam final List<String> accessions,
      @RequestHeader(name = "Authorization", required = false) final String token) {
    final Authentication loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final String principle = sampleService.getPrinciple(loggedInUser);

    if (accessions == null) {
      throw new GlobalExceptions.BulkFetchInvalidRequestException();
    }

    log.info("V2-Received request to bulk-fetch " + accessions.size() + " accessions");

    final List<Sample> samples =
        accessions.stream()
            .map(
                accession -> {
                  final String justAccession = accession.trim();
                  final Optional<Sample> sampleOptional =
                      // fetch returns sample with no-curations applied
                      sampleService.fetch(justAccession, false);

                  if (sampleOptional.isPresent()) {
                    final Sample sample = sampleOptional.get();

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
    final Authentication loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final String principle = sampleService.getPrinciple(loggedInUser);

    if (principle == null) {
      throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
    }

    log.info("V2-Received POST for " + samples.size() + " samples");

    final List<Sample> createdSamples = new ArrayList<>();
    final List<SubmissionReceipt.ErrorReceipt> errors = new ArrayList<>();

    for (final Sample sample : samples) {
      final Pair<Optional<Sample>, Optional<String>> sampleErrorPair =
          persistSample(principle, sample);

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
    log.info("V2-Received POST for " + samples.size() + " samples");

    final Authentication loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final String principle = sampleService.getPrinciple(loggedInUser);

    if (principle == null) {
      throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
    }

    if (!webinAuthenticationService.isWebinSuperUser(principle)) {
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "POST for super users only");
    }

    final List<Sample> createdSamples =
        samples.stream()
            .map(sample -> persistSampleNoValidation(principle, sample))
            .collect(Collectors.toList());

    log.info(
        "V2-Received bulk-submit request for : "
            + samples.size()
            + " samples and persisted : "
            + createdSamples.size()
            + " samples.");

    return ResponseEntity.status(HttpStatus.CREATED).body(createdSamples);
  }

  /*
  Submit multiple samples, without any relationship information
   */
  @PostMapping(
      value = "/bulk-validate",
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<SubmissionReceipt> validateV2(@RequestBody final List<Sample> samples) {
    log.info("V2-Received Validate request for " + samples.size() + " samples");

    final List<SubmissionReceipt.ErrorReceipt> errors = new ArrayList<>();
    List<SubmissionReceipt.ValidationError> validationErrors;

    for (final Sample sample : samples) {
      final String validationResult = validateGetMessages(sample);

      if (validationResult != null) {
        try {
          validationErrors = objectMapper.readValue(validationResult, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
          validationErrors =
              Collections.singletonList(
                  new SubmissionReceipt.ValidationError(
                      "", Collections.singletonList(validationResult)));
        }

        errors.add(new SubmissionReceipt.ErrorReceipt(sample.getName(), validationErrors));
      }
    }

    log.info(
        "V2-Received bulk-validate request for : "
            + samples.size()
            + " samples and validated : "
            + samples.size()
            + " samples.");

    return ResponseEntity.status(HttpStatus.OK).body(new SubmissionReceipt(null, errors));
  }

  private Pair<Optional<Sample>, Optional<String>> persistSample(
      final String principle, Sample sample) {
    final boolean isWebinSuperUser = webinAuthenticationService.isWebinSuperUser(principle);
    final Optional<Sample> oldSample =
        sampleService.validateSampleWithAccessionsAgainstConditionsAndGetOldSample(
            sample, isWebinSuperUser);
    final Set<Relationship> relationships =
        sampleService.handleSampleRelationshipsV2(sample, oldSample, isWebinSuperUser);

    sample = webinAuthenticationService.handleWebinUserSubmission(sample, principle, oldSample);

    sample = buildSample(sample, relationships, isWebinSuperUser);

    Pair<Optional<Sample>, Optional<String>> sampleErrorPair;

    try {
      if (bioSamplesProperties.isEnableBulkSubmissionWebinSuperUserValidation()
          || !isWebinSuperUser) {
        validate(sample);
      }

      final Optional<Sample> persistedSample =
          Optional.of(
              sampleService.persistSampleV2(sample, oldSample.orElse(null), isWebinSuperUser));
      sampleErrorPair = new ImmutablePair<>(persistedSample, Optional.empty());
    } catch (GlobalExceptions.SchemaValidationException e) {
      sampleErrorPair = new ImmutablePair<>(Optional.empty(), Optional.ofNullable(e.getMessage()));

      log.info("Sample validation failed: {}", sample.getAccession());
    } catch (Exception e) {
      sampleErrorPair = new ImmutablePair<>(Optional.empty(), Optional.ofNullable(e.getMessage()));

      log.error("Failed to validate sample", e);
    }

    return sampleErrorPair;
  }

  private Sample persistSampleNoValidation(final String principle, Sample sample) {
    final Optional<Sample> oldSample =
        sampleService.validateSampleWithAccessionsAgainstConditionsAndGetOldSample(sample, true);
    final Set<Relationship> relationships =
        sampleService.handleSampleRelationshipsV2(sample, oldSample, true);

    sample = webinAuthenticationService.handleWebinUserSubmission(sample, principle, oldSample);
    sample = buildSample(sample, relationships, true);

    return sampleService.persistSampleV2(sample, oldSample.orElse(null), true);
  }

  private String validateGetMessages(final Sample sample) {
    final String sampleIdentifier =
        sample.getAccession() != null ? sample.getAccession() : sample.getName();
    try {
      schemaValidationService.validate(sample);
    } catch (GlobalExceptions.SchemaValidationException e) {
      log.info("Sample validation failed: {}", sample.getAccession());

      return Optional.ofNullable(e.getMessage())
          .orElse("Unknown validation error while validating sample: " + sampleIdentifier);
    } catch (Exception e) {
      log.error("Failed to validate sample", e);

      return Optional.ofNullable(e.getMessage())
          .orElse("Unknown validation error while validating sample: " + sampleIdentifier);
    }

    return null;
  }

  private void validate(Sample sample) {
    schemaValidationService.validate(sample);
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
