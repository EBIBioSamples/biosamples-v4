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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.Certificate;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;
import uk.ac.ebi.biosamples.model.certification.BioSamplesCertificationComplainceResult;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.certification.CertifyService;

@RestController
@ExposesResourceFor(Sample.class)
@RequestMapping("/samples")
@CrossOrigin
public class CertificationController {
  private Logger log = LoggerFactory.getLogger(getClass());

  @Autowired private CertifyService certifyService;
  @Autowired private SampleService sampleService;
  @Autowired private BioSamplesAapService bioSamplesAapService;
  @Autowired private BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  @Autowired private SampleResourceAssembler sampleResourceAssembler;

  @PutMapping("{accession}/certify")
  public Resource<Sample> certify(
      HttpServletRequest request,
      @RequestBody Sample sample,
      @PathVariable String accession,
      @RequestParam(name = "authProvider", required = false, defaultValue = "AAP")
          String authProvider)
      throws JsonProcessingException {
    final ObjectMapper jsonMapper = new ObjectMapper();

    if (sample.getAccession() == null || !sample.getAccession().equals(accession)) {
      throw new SampleRestController.SampleAccessionMismatchException();
    }

    if (!authProvider.equalsIgnoreCase("WEBIN")) {
      if (sampleService.isNotExistingAccession(accession)
          && !(bioSamplesAapService.isWriteSuperUser()
              || bioSamplesAapService.isIntegrationTestUser())) {
        throw new SampleRestController.SampleAccessionDoesNotExistException();
      }
    }

    log.info("Received PUT for validation of " + accession);

    if (authProvider.equalsIgnoreCase("WEBIN")) {
      final BearerTokenExtractor bearerTokenExtractor = new BearerTokenExtractor();
      final Authentication authentication = bearerTokenExtractor.extract(request);
      final SubmissionAccount webinAccount =
          bioSamplesWebinAuthenticationService
              .getWebinSubmissionAccount(String.valueOf(authentication.getPrincipal()))
              .getBody();

      final String webinAccountId = webinAccount.getId();

      if (sampleService.isNotExistingAccession(accession)
          && !bioSamplesWebinAuthenticationService.isWebinSuperUser(webinAccountId)) {
        throw new SampleRestController.SampleAccessionDoesNotExistException();
      }

      sample = bioSamplesWebinAuthenticationService.handleWebinUser(sample, webinAccountId);
    } else {
      sample = bioSamplesAapService.handleSampleDomain(sample);
    }

    List<Certificate> certificates =
        certifyService.certify(jsonMapper.writeValueAsString(sample), true);

    // update date is system generated field
    Instant reviewed = Instant.now();

    SubmittedViaType submittedVia =
        sample.getSubmittedVia() == null ? SubmittedViaType.JSON_API : sample.getSubmittedVia();
    sample =
        Sample.Builder.fromSample(sample)
            .withCertificates(certificates)
            .withReviewed(reviewed)
            .withSubmittedVia(submittedVia)
            .build();

    log.trace("Sample with certificates " + sample);

    sample = sampleService.store(sample, false, authProvider);

    // assemble a resource to return
    // create the response object with the appropriate status
    return sampleResourceAssembler.toResource(sample);
  }

  @PostMapping("/checkCompliance")
  public BioSamplesCertificationComplainceResult recorderResults(@RequestBody Sample sample)
      throws JsonProcessingException {
    final ObjectMapper jsonMapper = new ObjectMapper();

    return certifyService.recordResult(jsonMapper.writeValueAsString(sample), true);
  }
}
