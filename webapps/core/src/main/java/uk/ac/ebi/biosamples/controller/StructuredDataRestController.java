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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.service.StructuredDataService;
import uk.ac.ebi.biosamples.service.security.AccessControlService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;

/** Structured data operations */
@RestController
@ExposesResourceFor(StructuredData.class)
@RequestMapping("/structureddata/{accession}")
@CrossOrigin
public class StructuredDataRestController {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final BioSamplesAapService bioSamplesAapService;
  private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  private final StructuredDataService structuredDataService;
  private final AccessControlService accessControlService;

  public StructuredDataRestController(
      BioSamplesAapService bioSamplesAapService,
      BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
      StructuredDataService structuredDataService,
      AccessControlService accessControlService) {
    this.bioSamplesAapService = bioSamplesAapService;
    this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
    this.structuredDataService = structuredDataService;
    this.accessControlService = accessControlService;
  }

  @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @GetMapping()
  public Resource<StructuredData> get(@PathVariable String accession) {
    if (accession == null || accession.isEmpty()) {
      throw new GlobalExceptions.SampleAccessionMismatchException();
    }

    return new Resource<>(
        structuredDataService
            .getStructuredData(accession)
            .orElseThrow(() -> new GlobalExceptions.SampleNotFoundException()));
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public Resource<StructuredData> put(
      @PathVariable String accession,
      @RequestBody StructuredData structuredData,
      @RequestHeader("Authorization") final String token) {
    final AuthToken authToken =
        accessControlService
            .extractToken(token)
            .orElseThrow(
                () ->
                    new GlobalExceptions.AccessControlException(
                        "Invalid token. Please provide valid token."));
    final boolean webinAuth = authToken.getAuthority() == AuthorizationProvider.WEBIN;

    log.info("PUT request for structured data: {}", accession);

    if (structuredData.getAccession() == null || !structuredData.getAccession().equals(accession)) {
      throw new GlobalExceptions.SampleAccessionMismatchException();
    }

    if (webinAuth) {
      bioSamplesWebinAuthenticationService
          .handleStructuredDataAccessibilityForOnlyStructuredDataSubmission(
              structuredData, authToken.getUser());
    } else {
      bioSamplesAapService.handleStructuredDataDomain(structuredData);
    }

    StructuredData storedData = structuredDataService.saveStructuredData(structuredData);

    return new Resource<>(storedData);
  }
}
