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
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.StructuredDataService;
import uk.ac.ebi.biosamples.service.security.WebinAuthenticationService;

/** Structured data operations */
@RestController
@ExposesResourceFor(StructuredData.class)
@RequestMapping("/structureddata/{accession}")
@CrossOrigin
public class StructuredDataController {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final WebinAuthenticationService webinAuthenticationService;
  private final StructuredDataService structuredDataService;
  private final SampleService sampleService;

  public StructuredDataController(
      final WebinAuthenticationService webinAuthenticationService,
      final StructuredDataService structuredDataService,
      final SampleService sampleService) {
    this.webinAuthenticationService = webinAuthenticationService;
    this.structuredDataService = structuredDataService;
    this.sampleService = sampleService;
  }

  @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @GetMapping()
  public EntityModel<StructuredData> get(@PathVariable final String accession) {
    if (accession == null || accession.isEmpty()) {
      throw new GlobalExceptions.SampleAccessionMismatchException();
    }

    return EntityModel.of(
        structuredDataService
            .getStructuredData(accession)
            .orElseThrow(GlobalExceptions.SampleNotFoundException::new));
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public EntityModel<StructuredData> put(
      @PathVariable final String accession, @RequestBody final StructuredData structuredData) {
    final Authentication loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final String principle = sampleService.getPrinciple(loggedInUser);

    if (principle == null) {
      throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
    }

    log.info("PUT request for structured data: {}", accession);

    if (structuredData.getAccession() == null || !structuredData.getAccession().equals(accession)) {
      throw new GlobalExceptions.SampleAccessionMismatchException();
    }

    webinAuthenticationService.isStructuredDataAccessible(structuredData, principle);

    return EntityModel.of(structuredDataService.saveStructuredData(structuredData));
  }
}
