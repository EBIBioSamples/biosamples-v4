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
import uk.ac.ebi.biosamples.service.SchemaValidationServiceV2;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapServiceV2;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationServiceV2;
import uk.ac.ebi.biosamples.service.taxonomy.ENATaxonClientServiceV2;

@RestController
@ExposesResourceFor(Sample.class)
@RequestMapping("/v2/samples")
@CrossOrigin
public class SamplesRestControllerV2 {
  private Logger log = LoggerFactory.getLogger(getClass());

  private final SampleServiceV2 sampleServiceV2;
  private final BioSamplesAapServiceV2 bioSamplesAapServiceV2;
  private final BioSamplesWebinAuthenticationServiceV2 bioSamplesWebinAuthenticationServiceV2;
  private final SchemaValidationServiceV2 schemaValidationServiceV2;
  private final ENATaxonClientServiceV2 enaTaxonClientServiceV2;

  public SamplesRestControllerV2(
      final SampleServiceV2 sampleServiceV2,
      final BioSamplesAapServiceV2 bioSamplesAapServiceV2,
      final BioSamplesWebinAuthenticationServiceV2 bioSamplesWebinAuthenticationServiceV2,
      final SchemaValidationServiceV2 schemaValidationServiceV2,
      final ENATaxonClientServiceV2 enaTaxonClientServiceV2) {
    this.sampleServiceV2 = sampleServiceV2;
    this.bioSamplesAapServiceV2 = bioSamplesAapServiceV2;
    this.bioSamplesWebinAuthenticationServiceV2 = bioSamplesWebinAuthenticationServiceV2;
    this.schemaValidationServiceV2 = schemaValidationServiceV2;
    this.enaTaxonClientServiceV2 = enaTaxonClientServiceV2;
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
          bioSamplesWebinAuthenticationServiceV2
              .getWebinSubmissionAccount(String.valueOf(authentication.getPrincipal()))
              .getBody();
      final String webinAccountId = webinAccount.getId();
      isWebinSuperUser = bioSamplesWebinAuthenticationServiceV2.isWebinSuperUser(webinAccountId);
      final boolean finalIsWebinSuperUser = isWebinSuperUser;

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(
              samples.stream()
                  .map(
                      sample -> {
                        sample =
                            bioSamplesWebinAuthenticationServiceV2.handleWebinUser(
                                sample, webinAccountId);

                        if (!finalIsWebinSuperUser) {
                          schemaValidationServiceV2.validate(sample);
                          sample = enaTaxonClientServiceV2.performTaxonomyValidation(sample);
                        }

                        return sampleServiceV2.store(sample, true, authProvider);
                      })
                  .collect(Collectors.toList()));
    } else {
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(
              samples.stream()
                  .map(
                      sample -> {
                        sample = bioSamplesAapServiceV2.handleSampleDomain(sample);

                        if (!bioSamplesAapServiceV2.isWriteSuperUser()) {
                          schemaValidationServiceV2.validate(sample);
                        }

                        return sampleServiceV2.store(sample, true, authProvider);
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
          bioSamplesWebinAuthenticationServiceV2
              .getWebinSubmissionAccount(String.valueOf(authentication.getPrincipal()))
              .getBody();

      sample = bioSamplesWebinAuthenticationServiceV2.handleWebinUser(sample, webinAccount.getId());
    } else {
      sample = bioSamplesAapServiceV2.handleSampleDomain(sample);
    }

    sample = buildPrivateSampleV2(sample);

    sample = sampleServiceV2.store(sample, false, authProvider);

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
    log.debug("Received POST for bulk accessioning of " + samples.size() + " samples");

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
          bioSamplesWebinAuthenticationServiceV2
              .getWebinSubmissionAccount(String.valueOf(authentication.getPrincipal()))
              .getBody();

      samples =
          samples.stream()
              .map(
                  sample ->
                      bioSamplesWebinAuthenticationServiceV2.getSampleWithWebinSubmissionAccountId(
                          sample, webinAccount.getId()))
              .collect(Collectors.toList());
    } else {
      if (samples.size() > 0) {
        Sample firstSample = samples.get(0);
        firstSample = bioSamplesAapServiceV2.handleSampleDomain(firstSample);

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
                  log.trace("Initiating store() for " + sample.getName());
                  sample = buildPrivateSampleV2(sample);
                  return sampleServiceV2.store(sample, false, authProvider);
                })
            .collect(Collectors.toList());

    final Map<String, String> outputMap =
        createdSamplesList.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Sample::getName, Sample::getAccession));

    return ResponseEntity.ok(outputMap);
  }

  @ResponseStatus(
      value = HttpStatus.BAD_REQUEST,
      reason = "New sample submission should not contain an accession")
  public static class SampleWithAccessionSubmissionExceptionV2 extends RuntimeException {}
}
