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

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.exception.SampleAccessionMismatchException;
import uk.ac.ebi.biosamples.exception.SampleNotFoundException;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.service.StructuredDataService;
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

  public StructuredDataRestController(
      BioSamplesAapService bioSamplesAapService,
      BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
      StructuredDataService structuredDataService) {
    this.bioSamplesAapService = bioSamplesAapService;
    this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
    this.structuredDataService = structuredDataService;
  }

  @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @GetMapping()
  public Resource<StructuredData> get(@PathVariable String accession) {
    if (accession == null || accession.isEmpty()) {
      throw new SampleAccessionMismatchException();
    }

    return new Resource<>(
        structuredDataService
            .getStructuredData(accession)
            .orElseThrow(() -> new SampleNotFoundException()));
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public Resource<StructuredData> put(
      HttpServletRequest request,
      @PathVariable String accession,
      @RequestBody StructuredData structuredData,
      @RequestParam(name = "authProvider", required = false, defaultValue = "AAP")
          String authProvider) {

    log.info("PUT request for structured data: {}", accession);
    if (structuredData.getAccession() == null || !structuredData.getAccession().equals(accession)) {
      throw new SampleAccessionMismatchException();
    }

    if ("WEBIN".equalsIgnoreCase(authProvider)) {
      final BearerTokenExtractor bearerTokenExtractor = new BearerTokenExtractor();
      final Authentication authentication = bearerTokenExtractor.extract(request);
      final SubmissionAccount webinAccount =
          bioSamplesWebinAuthenticationService
              .getWebinSubmissionAccount(String.valueOf(authentication.getPrincipal()))
              .getBody();
      bioSamplesWebinAuthenticationService.handleStructuredDataAccesibility(
          structuredData, webinAccount.getId());
    } else {
      bioSamplesAapService.handleStructuredDataDomain(structuredData);
    }

    StructuredData storedData = structuredDataService.saveStructuredData(structuredData);
    return new Resource<>(storedData);
  }
}