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
package uk.ac.ebi.biosamples.service.security;

import java.util.Optional;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;
import uk.ac.ebi.biosamples.service.SampleServiceV2;

@Service
public class BioSamplesWebinAuthenticationServiceV2 {
  private final RestTemplate restTemplate;
  private final SampleServiceV2 sampleServiceV2;
  private final BioSamplesProperties bioSamplesProperties;

  public BioSamplesWebinAuthenticationServiceV2(
      SampleServiceV2 sampleServiceV2, BioSamplesProperties bioSamplesProperties) {
    this.restTemplate = new RestTemplate();
    this.sampleServiceV2 = sampleServiceV2;
    this.bioSamplesProperties = bioSamplesProperties;
  }

  public ResponseEntity<SubmissionAccount> getWebinSubmissionAccount(final String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + token);
    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<SubmissionAccount> responseEntity =
          restTemplate.exchange(
              bioSamplesProperties.getBiosamplesWebinAuthFetchSubmissionAccountUri(),
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

  public Sample handleWebinUser(Sample sample, String webinId) {
    final String biosamplesClientWebinUsername =
        bioSamplesProperties.getBiosamplesClientWebinUsername();

    if (webinId != null && !webinId.isEmpty()) { // webin id retrieval failure - throw Exception
      if (sample.getAccession() != null) { // sample updates, where sample has an accession
        if (webinId.equalsIgnoreCase(
            biosamplesClientWebinUsername)) { // ENA pipeline submissions, check if submission done
          // by internal client program (1)
          final String webinSubmissionAccountIdInMetadata =
              sample
                  .getWebinSubmissionAccountId(); // if (1) is true, override submission account id
          // in
          // sample with original account id from sample metadata

          return getSampleWithWebinSubmissionAccountId(
              sample,
              (webinSubmissionAccountIdInMetadata != null
                      && !webinSubmissionAccountIdInMetadata.isEmpty())
                  ? webinSubmissionAccountIdInMetadata
                  : biosamplesClientWebinUsername);
        } else { // normal sample update - not pipeline, check for old user, if mismatch throw
          // exception, else build the Sample
          Optional<Sample> oldSample =
              sampleServiceV2.fetch(sample.getAccession(), Optional.empty(), null);

          if (oldSample.isPresent()) {
            final Sample oldSavedSample = oldSample.get();

            if (!webinId.equalsIgnoreCase(
                oldSavedSample.getWebinSubmissionAccountId())) { // original submitter mismatch
              throw new SampleNotAccessibleException();
            } else {
              return getSampleWithWebinSubmissionAccountId(sample, webinId);
            }
          } else {
            return getSampleWithWebinSubmissionAccountId(sample, webinId);
          }
        }
      } else { // new submission
        if (webinId.equalsIgnoreCase(
            biosamplesClientWebinUsername)) { // new submission by client program (2)
          final String webinSubmissionAccountIdInMetadata =
              sample
                  .getWebinSubmissionAccountId(); // if true (2), override submission account id in
          // sample with original account id from sample metadata

          return getSampleWithWebinSubmissionAccountId(
              sample,
              (webinSubmissionAccountIdInMetadata != null
                      && !webinSubmissionAccountIdInMetadata.isEmpty())
                  ? webinSubmissionAccountIdInMetadata
                  : biosamplesClientWebinUsername);
        } else {
          return getSampleWithWebinSubmissionAccountId(sample, webinId);
        }
      }
    } else {
      throw new WebinUserLoginUnauthorizedException();
    }
  }

  public boolean isWebinSuperUser(String webinId) {
    return webinId.equalsIgnoreCase(bioSamplesProperties.getBiosamplesClientWebinUsername());
  }

  public Sample getSampleWithWebinSubmissionAccountId(Sample sample, String webinId) {
    return Sample.Builder.fromSample(sample)
        .withWebinSubmissionAccountId(webinId)
        .withNoDomain()
        .build();
  }

  @ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Unauthorized WEBIN user")
  public static class WebinUserLoginUnauthorizedException extends RuntimeException {}

  @ResponseStatus(
      value = HttpStatus.FORBIDDEN,
      reason =
          "This sample is private and not available for browsing. If you think this is an error and/or you should have access please contact the BioSamples Helpdesk at biosamples@ebi.ac.uk")
  public static class SampleNotAccessibleException extends RuntimeException {}
}
