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

import java.util.Collections;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.AccessControlService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;

@RestController
@ExposesResourceFor(Sample.class)
@RequestMapping("/samples/{accession}")
@CrossOrigin
public class SampleGetControllerV2 {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final SampleService sampleService;
  private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  private final AccessControlService accessControlService;

  public SampleGetControllerV2(
      final SampleService sampleService,
      final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
      final AccessControlService accessControlService) {
    this.sampleService = sampleService;
    this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
    this.accessControlService = accessControlService;
  }

  /*
  Fetch single sample
   */
  @PreAuthorize("isAuthenticated()")
  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public Sample getSampleV2(
      @PathVariable final String accession,
      @RequestHeader(name = "Authorization", required = false) final String token) {
    log.info("Received fetch for accession " + accession);

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final Optional<Sample> optionalSample =
        sampleService.fetch(accession, Optional.of(Collections.singletonList("")));

    if (optionalSample.isPresent()) {
      final Sample sample = optionalSample.get();
      final String webinSubmissionAccountId = authToken.get().getUser();

      bioSamplesWebinAuthenticationService.isSampleAccessible(sample, webinSubmissionAccountId);

      return sample;
    } else {
      throw new GlobalExceptions.SampleNotFoundException();
    }
  }
}
