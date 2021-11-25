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
import java.util.concurrent.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;
import uk.ac.ebi.biosamples.service.SampleServiceV2;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.service.taxonomy.ENATaxonClientService;
import uk.ac.ebi.biosamples.validation.SchemaValidationService;

@RestController
@ExposesResourceFor(Sample.class)
@RequestMapping("/v2/samples")
@CrossOrigin
public class SamplesRestControllerV2 {
  private Logger log = LoggerFactory.getLogger(getClass());

  private final SampleServiceV2 sampleService;
  private final BioSamplesAapService bioSamplesAapService;
  private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  private final SchemaValidationService schemaValidationService;
  private final ENATaxonClientService enaTaxonClientService;

  private final int maxThreads = 10;

  public SamplesRestControllerV2(
      final SampleServiceV2 sampleService,
      final BioSamplesAapService bioSamplesAapService,
      final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
      final SchemaValidationService schemaValidationService,
      final ENATaxonClientService enaTaxonClientService) {
    this.sampleService = sampleService;
    this.bioSamplesAapService = bioSamplesAapService;
    this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
    this.schemaValidationService = schemaValidationService;
    this.enaTaxonClientService = enaTaxonClientService;
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(
      value = "/submit",
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<List<Sample>> postSamplesV2(
      final HttpServletRequest request,
      @RequestBody final List<Sample> samples,
      @RequestParam(name = "authProvider", required = false, defaultValue = "AAP")
          final String authProvider) {
    log.info("Received POST for " + samples.size() + " samples");

    final boolean webinAuth = authProvider.equalsIgnoreCase("WEBIN");
    boolean isWebinSuperUser;

    if (webinAuth) {
      final BearerTokenExtractor bearerTokenExtractor = new BearerTokenExtractor();

      final Authentication authentication = bearerTokenExtractor.extract(request);
      final SubmissionAccount webinAccount =
          bioSamplesWebinAuthenticationService
              .getWebinSubmissionAccount(String.valueOf(authentication.getPrincipal()))
              .getBody();
      final String webinAccountId = webinAccount.getId();
      isWebinSuperUser = bioSamplesWebinAuthenticationService.isWebinSuperUser(webinAccountId);
      final boolean finalIsWebinSuperUser = isWebinSuperUser;

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(
              samples.stream()
                  .map(
                      sample -> {
                        sample =
                            bioSamplesWebinAuthenticationService.handleWebinUser(
                                sample, webinAccountId);

                        if (!finalIsWebinSuperUser) {
                          schemaValidationService.validate(sample);
                          sample = enaTaxonClientService.performTaxonomyValidation(sample);
                        }

                        return sampleService.store(sample, true, authProvider);
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
                        }

                        return sampleService.store(sample, true, authProvider);
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
      final HttpServletRequest request,
      @RequestBody Sample sample,
      @RequestParam(name = "authProvider", required = false, defaultValue = "AAP")
          String authProvider) {
    log.debug("Received POST for accessioning " + sample);
    if (sample.hasAccession()) throw new SampleWithAccessionSubmissionExceptionV2();

    if (authProvider.equalsIgnoreCase("WEBIN")) {
      final BearerTokenExtractor bearerTokenExtractor = new BearerTokenExtractor();
      final Authentication authentication = bearerTokenExtractor.extract(request);
      final SubmissionAccount webinAccount =
          bioSamplesWebinAuthenticationService
              .getWebinSubmissionAccount(String.valueOf(authentication.getPrincipal()))
              .getBody();

      sample = bioSamplesWebinAuthenticationService.handleWebinUser(sample, webinAccount.getId());
    } else {
      sample = bioSamplesAapService.handleSampleDomain(sample);
    }

    sample = buildPrivateSampleV2(sample);

    sample = sampleService.store(sample, false, authProvider);

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
      HttpServletRequest request,
      @RequestBody List<Sample> samples,
      @RequestParam(name = "authProvider", required = false, defaultValue = "AAP")
          final String authProvider) {
    log.info("Received POST for bulk accessioning of " + samples.size() + " samples");

    try {
      samples.forEach(
          sample -> {
            if (sample.hasAccession()) {
              throw new SampleWithAccessionSubmissionExceptionV2();
            }
          });

      if (authProvider.equalsIgnoreCase("WEBIN")) {
        final BearerTokenExtractor bearerTokenExtractor = new BearerTokenExtractor();
        final Authentication authentication = bearerTokenExtractor.extract(request);
        final SubmissionAccount webinAccount =
            bioSamplesWebinAuthenticationService
                .getWebinSubmissionAccount(String.valueOf(authentication.getPrincipal()))
                .getBody();

        samples =
            samples.stream()
                .map(
                    sample ->
                        bioSamplesWebinAuthenticationService.getSampleWithWebinSubmissionAccountId(
                            sample, webinAccount.getId()))
                .collect(Collectors.toList());
      } else {
        if (samples.size() > 0) {
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

      final ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
      final List<Future<Sample>> sampleFutures =
          samples.stream()
              .map(sample -> executor.submit(new SamplePersistence(sample, authProvider)))
              .collect(Collectors.toList());

      log.info("Number of samples created " + sampleFutures.size());

      final Map<String, String> outputMap =
          sampleFutures.stream()
              .map(
                  sampleFuture -> {
                    try {
                      return sampleFuture.get();
                    } catch (InterruptedException | ExecutionException e) {
                      log.info("Exception here " + e.getMessage());
                    }

                    return null;
                  })
              .filter(Objects::nonNull)
              .collect(Collectors.toMap(Sample::getName, Sample::getAccession));

      /*final List<Sample> createdSamplesList =
          samples.stream()
              .map(
                  sample -> {
                    log.trace("Initiating store() for " + sample.getName());
                    sample = buildPrivateSampleV2(sample);
                    return sampleService.store(sample, false, authProvider);
                  })
              .collect(Collectors.toList());

      final Map<String, String> outputMap =
          createdSamplesList.stream()
              .filter(Objects::nonNull)
              .collect(Collectors.toMap(Sample::getName, Sample::getAccession));*/

      return ResponseEntity.ok(outputMap);
    } catch (final Exception e) {
      log.info("Failed to assign accessions to " + samples.size() + " samples");

      throw new BulkAccessionFailureExceptionV2(e.getMessage());
    }
  }

  class SamplePersistence implements Callable<Sample> {
    Sample sample;
    String authProvider;

    SamplePersistence(Sample sample, String authProvider) {
      this.sample = sample;
      this.authProvider = authProvider;
    }

    @Override
    public Sample call() {
      Logger log = LoggerFactory.getLogger(getClass());

      final Instant release =
          Instant.ofEpochSecond(
              LocalDateTime.now(ZoneOffset.UTC).plusYears(100).toEpochSecond(ZoneOffset.UTC));
      final Instant update = Instant.now();
      final SubmittedViaType submittedVia =
          sample.getSubmittedVia() == null ? SubmittedViaType.JSON_API : sample.getSubmittedVia();

      sample =
          Sample.Builder.fromSample(sample)
              .withRelease(release)
              .withUpdate(update)
              .withSubmittedVia(submittedVia)
              .build();

      log.info("Initiating store() for " + sample.getName());

      return sampleService.store(sample, false, authProvider);
    }
  }

  @ResponseStatus(
      value = HttpStatus.BAD_REQUEST,
      reason = "New sample submission should not contain an accession")
  public static class SampleWithAccessionSubmissionExceptionV2 extends RuntimeException {}

  @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE, reason = "Bulk accessioning failure")
  public static class BulkAccessionFailureExceptionV2 extends RuntimeException {
    public BulkAccessionFailureExceptionV2(String message) {
      super(message);
    }
  }
}
