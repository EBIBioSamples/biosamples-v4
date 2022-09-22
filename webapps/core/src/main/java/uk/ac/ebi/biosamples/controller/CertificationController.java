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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.Certificate;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.model.certification.BioSamplesCertificationComplainceResult;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.certification.CertifyService;
import uk.ac.ebi.biosamples.service.security.AccessControlService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;

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
  @Autowired private AccessControlService accessControlService;

  @PutMapping("{accession}/certify")
  public EntityModel<Sample> certify(
      @RequestBody Sample sample,
      @PathVariable String accession,
      @RequestHeader(name = "Authorization", required = false) final String token)
      throws JsonProcessingException {

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final boolean webinAuth =
        authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE);
    final AuthorizationProvider authProvider =
        webinAuth ? AuthorizationProvider.WEBIN : AuthorizationProvider.AAP;

    final ObjectMapper jsonMapper = new ObjectMapper();

    if (sample.getAccession() == null || !sample.getAccession().equals(accession)) {
      throw new GlobalExceptions.SampleAccessionMismatchException();
    }

    if (!webinAuth) {
      if (sampleService.isNotExistingAccession(accession)
          && !(bioSamplesAapService.isWriteSuperUser()
              || bioSamplesAapService.isIntegrationTestUser())) {
        throw new GlobalExceptions.SampleAccessionDoesNotExistException();
      }
    }

    log.info("Received PUT for validation of " + accession);

    if (webinAuth) {
      final String webinSubmissionAccountId = authToken.get().getUser();

      if (sampleService.isNotExistingAccession(accession)
          && !bioSamplesWebinAuthenticationService.isWebinSuperUser(webinSubmissionAccountId)) {
        throw new GlobalExceptions.SampleAccessionDoesNotExistException();
      }

      sample =
          bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
              sample, webinSubmissionAccountId, Optional.empty());
    } else {
      sample = bioSamplesAapService.handleSampleDomain(sample, Optional.empty());
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

    sample = sampleService.persistSample(sample, null, authProvider, false);

    // assemble a resource to return
    // create the response object with the appropriate status
    return sampleResourceAssembler.toModel(sample);
  }

  @PostMapping("/checkCompliance")
  public BioSamplesCertificationComplainceResult recorderResults(@RequestBody Sample sample)
      throws JsonProcessingException {
    final ObjectMapper jsonMapper = new ObjectMapper();

    return certifyService.recordResult(jsonMapper.writeValueAsString(sample), true);
  }
}
