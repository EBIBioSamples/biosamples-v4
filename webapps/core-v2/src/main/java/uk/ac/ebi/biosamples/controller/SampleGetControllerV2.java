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

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.WebinAuthenticationService;

@RestController
@ExposesResourceFor(Sample.class)
@RequestMapping("/samples/{accession}")
@CrossOrigin
public class SampleGetControllerV2 {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final SampleService sampleService;
  private final WebinAuthenticationService webinAuthenticationService;

  public SampleGetControllerV2(
      final SampleService sampleService,
      final WebinAuthenticationService webinAuthenticationService) {
    this.sampleService = sampleService;
    this.webinAuthenticationService = webinAuthenticationService;
  }

  /*
  Fetch single sample
   */
  @PreAuthorize("isAuthenticated()")
  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public Sample getSampleV2(@PathVariable final String accession) {
    final Authentication loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final String principle = sampleService.getPrinciple(loggedInUser);

    log.info("Received fetch for accession " + accession);

    // fetch returns sample with no-curations applied
    final Optional<Sample> optionalSample = sampleService.fetch(accession, false);

    if (optionalSample.isPresent()) {
      final Sample sample = optionalSample.get();

      webinAuthenticationService.isSampleAccessible(sample, principle);

      return sample;
    } else {
      throw new GlobalExceptions.SampleNotFoundException();
    }
  }
}
