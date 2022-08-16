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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.AccessControlService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;

@RestController
@ExposesResourceFor(Sample.class)
@RequestMapping("/samples")
@CrossOrigin
public class BulkActionControllerV2 {
  private Logger log = LoggerFactory.getLogger(getClass());

  private final SampleService sampleService;
  private final BioSamplesAapService bioSamplesAapService;
  private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  private final AccessControlService accessControlService;

  public BulkActionControllerV2(
      final SampleService sampleService,
      final BioSamplesAapService bioSamplesAapService,
      final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
      final AccessControlService accessControlService) {
    this.sampleService = sampleService;
    this.bioSamplesAapService = bioSamplesAapService;
    this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
    this.accessControlService = accessControlService;
  }

  /*
  Bulk accession multiple samples
   */
  @PreAuthorize("isAuthenticated()")
  @PostMapping(
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @RequestMapping("/bulk-accession")
  public ResponseEntity<Map<String, String>> bulkAccessionSampleV2(
      @RequestBody List<Sample> samples,
      @RequestHeader(name = "Authorization") final String token) {
    log.info("V2-Received POST for bulk accessioning of " + samples.size() + " samples");

    samples.forEach(
        sample -> {
          if (sample.hasAccession()) {
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
                      bioSamplesWebinAuthenticationService.buildSampleWithWebinSubmissionAccountId(
                          sample, webinSubmissionAccountId))
              .collect(Collectors.toList());
    } else {
      if (!samples.isEmpty()) {
        // check the first sample domain only
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

                  sample = sampleService.buildPrivateSample(sample);
                  /*
                  Call the accessionSample from SampleService, it doesn't do a lot of housekeeping like reporting to Rabbit,
                  saving to MongoSampleCurated etc which is not required for bulk-accessioning
                   */
                  return sampleService.accessionSample(sample);
                })
            .collect(Collectors.toList());

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
  public ResponseEntity<Map<String, Sample>> getMultipleSamplesV2(
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
                      sampleService.fetch(cleanAccession, Optional.empty(), "");

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
                        bioSamplesAapService.checkSampleAccessibility(sample);
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
            .collect(Collectors.toList());

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
}
