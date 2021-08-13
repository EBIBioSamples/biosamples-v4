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
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class SampleRestControllerV2 {
  private Logger log = LoggerFactory.getLogger(getClass());

  private final SampleServiceV2 sampleServiceV2;
  private final BioSamplesAapServiceV2 bioSamplesAapServiceV2;
  private final BioSamplesWebinAuthenticationServiceV2 bioSamplesWebinAuthenticationServiceV2;
  private final SchemaValidationServiceV2 schemaValidationServiceV2;
  private final ENATaxonClientServiceV2 enaTaxonClientServiceV2;

  public SampleRestControllerV2(
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
  @PutMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Sample> putSampleV2(
      HttpServletRequest request,
      @PathVariable final String accession,
      @RequestBody Sample sample,
      @RequestParam(name = "authProvider", required = false, defaultValue = "AAP")
          final String authProvider) {
    final boolean webinAuth = authProvider.equalsIgnoreCase("WEBIN");
    boolean isWebinSuperUser = false;

    if (sample.getAccession() == null || !sample.getAccession().equals(accession)) {
      throw new SampleAccessionMismatchExceptionV2();
    }

    log.debug("Received PUT for " + accession);

    if (authProvider.equalsIgnoreCase("WEBIN")) {
      final BearerTokenExtractor bearerTokenExtractor = new BearerTokenExtractor();
      final Authentication authentication = bearerTokenExtractor.extract(request);
      final SubmissionAccount webinAccount =
          bioSamplesWebinAuthenticationServiceV2
              .getWebinSubmissionAccount(String.valueOf(authentication.getPrincipal()))
              .getBody();

      final String webinAccountId = webinAccount.getId();

      isWebinSuperUser = bioSamplesWebinAuthenticationServiceV2.isWebinSuperUser(webinAccountId);

      if (sampleServiceV2.isNotExistingAccession(accession) && !isWebinSuperUser) {
        throw new SampleAccessionMismatchExceptionV2();
      }

      sample = bioSamplesWebinAuthenticationServiceV2.handleWebinUser(sample, webinAccountId);
    } else {
      if (sampleServiceV2.isNotExistingAccession(accession)
          && !(bioSamplesAapServiceV2.isWriteSuperUser()
              || bioSamplesAapServiceV2.isIntegrationTestUser())) {
        throw new SampleAccessionMismatchExceptionV2();
      }

      sample = bioSamplesAapServiceV2.handleSampleDomain(sample);
    }

    final Instant now = Instant.now();
    final SubmittedViaType submittedVia =
        sample.getSubmittedVia() == null ? SubmittedViaType.JSON_API : sample.getSubmittedVia();

    sample =
        Sample.Builder.fromSample(sample).withUpdate(now).withSubmittedVia(submittedVia).build();

    // Dont validate superuser samples, this helps to submit external (eg. NCBI, ENA) samples
    if (webinAuth && !isWebinSuperUser) {
      schemaValidationServiceV2.validate(sample);
    } else if (!webinAuth && !bioSamplesAapServiceV2.isWriteSuperUser()) {
      schemaValidationServiceV2.validate(sample);
    }

    if (submittedVia == SubmittedViaType.FILE_UPLOADER) {
      schemaValidationServiceV2.validate(sample);
    }

    if (webinAuth && !isWebinSuperUser) {
      sample = enaTaxonClientServiceV2.performTaxonomyValidation(sample);
    }

    final boolean isFirstTimeMetadataAdded = sampleServiceV2.beforeStore(sample);

    if (isFirstTimeMetadataAdded) {
      sample = Sample.Builder.fromSample(sample).withSubmitted(now).build();
    }

    sample = sampleServiceV2.store(sample, isFirstTimeMetadataAdded, authProvider);

    return ResponseEntity.status(HttpStatus.OK).body(sample);
  }

  @ResponseStatus(
      value = HttpStatus.BAD_REQUEST,
      reason = "Sample accession must match URL accession") // 400
  public static class SampleAccessionMismatchExceptionV2 extends RuntimeException {}
}
