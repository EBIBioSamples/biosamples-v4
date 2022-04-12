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
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.AccessControlService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.service.taxonomy.TaxonomyClientService;
import uk.ac.ebi.biosamples.validation.SchemaValidationService;

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

  @PreAuthorize("isAuthenticated()")
  @PutMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Sample> putSampleV2(
      HttpServletRequest request,
      @PathVariable final String accession,
      @RequestBody Sample sample,
      @RequestHeader(name = "Authorization") final String token) {

    final boolean webinAuth =
        accessControlService
            .extractToken(token)
            .map(t -> t.getAuthority() == AuthorizationProvider.WEBIN)
            .orElse(Boolean.FALSE);
    AuthorizationProvider authProvider =
        webinAuth ? AuthorizationProvider.WEBIN : AuthorizationProvider.AAP;

    boolean isWebinSuperUser = false;

    if (sample.getAccession() == null || !sample.getAccession().equals(accession)) {
      throw new GlobalExceptions.SampleAccessionMismatchException();
    }

    log.debug("Received PUT for " + accession);

    if (webinAuth) {
      final SubmissionAccount webinAccount =
          bioSamplesWebinAuthenticationService.getWebinSubmissionAccount(request);

      if (webinAccount == null) {
        throw new GlobalExceptions.WebinTokenInvalidException();
      }

      final String webinAccountId = webinAccount.getId();

      isWebinSuperUser = bioSamplesWebinAuthenticationService.isWebinSuperUser(webinAccountId);

      if (sampleService.isNotExistingAccession(accession) && !isWebinSuperUser) {
        throw new GlobalExceptions.SampleAccessionMismatchException();
      }

      sample = bioSamplesWebinAuthenticationService.handleWebinUser(sample, webinAccountId);
    } else {
      if (sampleService.isNotExistingAccession(accession)
          && !(bioSamplesAapService.isWriteSuperUser()
              || bioSamplesAapService.isIntegrationTestUser())) {
        throw new GlobalExceptions.SampleAccessionMismatchException();
      }

      sample = bioSamplesAapService.handleSampleDomain(sample);
    }

    final Instant now = Instant.now();
    final SubmittedViaType submittedVia =
        sample.getSubmittedVia() == null ? SubmittedViaType.JSON_API : sample.getSubmittedVia();

    sample =
        Sample.Builder.fromSample(sample).withUpdate(now).withSubmittedVia(submittedVia).build();

    // Dont validate superuser samples, this helps to submit external (eg. NCBI, ENA) samples
    if (webinAuth && !isWebinSuperUser) {
      schemaValidationService.validate(sample);
      sample = taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(sample, true);
    } else if (!webinAuth && !bioSamplesAapService.isWriteSuperUser()) {
      schemaValidationService.validate(sample);
      sample = taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(sample, false);
    }

    if (submittedVia == SubmittedViaType.FILE_UPLOADER) {
      schemaValidationService.validate(sample);
    }

    final boolean isFirstTimeMetadataAdded = sampleService.beforeStore(sample, isWebinSuperUser);

    if (isFirstTimeMetadataAdded) {
      sample = Sample.Builder.fromSample(sample).withSubmitted(now).build();
    }

    sample = sampleService.storeV2(sample, isFirstTimeMetadataAdded, authProvider);

    return ResponseEntity.status(HttpStatus.OK).body(sample);
  }
}
