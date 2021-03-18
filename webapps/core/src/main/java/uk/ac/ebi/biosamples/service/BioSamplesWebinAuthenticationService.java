/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.service;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;

@Service
public class BioSamplesWebinAuthenticationService {
  private Logger log = LoggerFactory.getLogger(getClass());

  private final RestTemplate restTemplate;
  private final SampleService sampleService;
  // private final BioSamplesProperties bioSamplesProperties;

  public BioSamplesWebinAuthenticationService(SampleService sampleService) {
    this.restTemplate = new RestTemplate();
    this.sampleService = sampleService;
    // this.bioSamplesProperties = bioSamplesProperties;
  }

  public ResponseEntity<SubmissionAccount> getWebinSubmissionAccount(final String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + token);
    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<SubmissionAccount> responseEntity =
          restTemplate.exchange(
              "https://www.ebi.ac.uk/ena/submit/webin/auth/admin/submission-account/",
              HttpMethod.GET,
              entity,
              SubmissionAccount.class);
      if (responseEntity.getStatusCode() == HttpStatus.OK) {
        return responseEntity;
      } else {
        return null;
      }
    } catch (final Exception e) {
      throw new WebinUserLoginUnauthorizedException();
    }
  }

  public ResponseEntity<String> getWebinToken(final String authRequest) {
    HttpHeaders headers = new HttpHeaders();
    System.out.println(authRequest);
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(authRequest, headers);

    try {
      ResponseEntity<String> responseEntity =
          restTemplate.exchange(
              "https://www.ebi.ac.uk/ena/submit/webin/auth/token",
              HttpMethod.POST,
              entity,
              String.class);
      if (responseEntity.getStatusCode() == HttpStatus.OK) {
        return responseEntity;
      } else {
        return null;
      }
    } catch (final Exception e) {
      throw new WebinUserLoginUnauthorizedException();
    }
  }

  public Sample handleWebinUser(Sample sample, String webinId) {
    if (webinId != null && !webinId.isEmpty()) {
      if (sample.getAccession() != null) {
        Optional<Sample> oldSample =
                sampleService.fetch(sample.getAccession(), Optional.empty(), null);
        if (oldSample.isEmpty()
                || !webinId
                .equalsIgnoreCase(oldSample.get().getWebinSubmissionAccountId())) {
          throw new BioSamplesAapService.SampleNotAccessibleException();
        }
      }

      return Sample.Builder.fromSample(sample)
              .withWebinSubmissionAccountId(webinId)
              .withNoDomain()
              .build();
    } else {
      throw new WebinUserLoginUnauthorizedException();
    }
  }

  public CurationLink handleWebinUser(CurationLink curationLink, String webinId) {
    if (webinId != null && !webinId.isEmpty()) {
      return CurationLink.build(
              curationLink.getSample(),
              curationLink.getCuration(),
              null,
              webinId,
              curationLink.getCreated());
    } else {
      throw new BioSamplesAapService.SampleNotAccessibleException();
    }
  }

  @ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Unauthorized WEBIN user")
  private static class WebinUserLoginUnauthorizedException extends RuntimeException {}
}
