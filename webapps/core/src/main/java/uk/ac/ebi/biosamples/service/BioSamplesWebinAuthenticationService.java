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

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataType;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class BioSamplesWebinAuthenticationService {
  private final RestTemplate restTemplate;
  private final SampleService sampleService;
  private final BioSamplesProperties bioSamplesProperties;

  public BioSamplesWebinAuthenticationService(
      SampleService sampleService, BioSamplesProperties bioSamplesProperties) {
    this.restTemplate = new RestTemplate();
    this.sampleService = sampleService;
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

  public ResponseEntity<String> getWebinToken(final String authRequest) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(authRequest, headers);

    try {
      ResponseEntity<String> responseEntity =
          restTemplate.exchange(
              bioSamplesProperties.getBiosamplesWebinAuthTokenUri(),
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
    final String biosamplesClientWebinUsername =
            bioSamplesProperties.getBiosamplesClientWebinUsername();

    if (webinId != null && !webinId.isEmpty()) { // webin id retrieval failure - throw Exception
      if (sample.getAccession() != null) { // sample updates, where sample has an accession
        if (webinId.equalsIgnoreCase(
                biosamplesClientWebinUsername)) { // ENA pipeline submissions, check if submission done
          // by internal client program (1)
          final String webinSubmissionAccountIdInMetadata =
                  sample.getWebinSubmissionAccountId(); // if (1) is true, override submission account id in
          // sample with original account id from sample metadata

          return getSampleWithWebinSubmissionAccountIdAdded(
                  sample,
                  (webinSubmissionAccountIdInMetadata != null
                          && !webinSubmissionAccountIdInMetadata.isEmpty())
                          ? webinSubmissionAccountIdInMetadata
                          : biosamplesClientWebinUsername);
        } else { // normal sample update - not pipeline, check for old user, if mismatch throw
          // exception, else build the Sample
          Optional<Sample> oldSample =
                  sampleService.fetch(sample.getAccession(), Optional.empty(), null);

          if (oldSample.isPresent()) {
            final Sample oldSavedSample = oldSample.get();

            if (!webinId.equalsIgnoreCase(
                    oldSavedSample.getWebinSubmissionAccountId())) { // original submitter mismatch
              throw new SampleNotAccessibleException();
            } else {
              return getSampleWithWebinSubmissionAccountIdAdded(sample, webinId);
            }
          } else {
            return getSampleWithWebinSubmissionAccountIdAdded(sample, webinId);
          }
        }
      } else { // new submission
        if (webinId.equalsIgnoreCase(
                biosamplesClientWebinUsername)) { // new submission by client program (2)
          final String webinSubmissionAccountIdInMetadata =
                  sample.getWebinSubmissionAccountId(); // if true (2), override submission account id in
          // sample with original account id from sample metadata

          return getSampleWithWebinSubmissionAccountIdAdded(
                  sample,
                  (webinSubmissionAccountIdInMetadata != null
                          && !webinSubmissionAccountIdInMetadata.isEmpty())
                          ? webinSubmissionAccountIdInMetadata
                          : biosamplesClientWebinUsername);
        } else {
          return getSampleWithWebinSubmissionAccountIdAdded(sample, webinId);
        }
      }
    } else {
      throw new WebinUserLoginUnauthorizedException();
    }
  }

  public Sample getSampleWithWebinSubmissionAccountIdAdded(Sample sample, String webinId) {
    return Sample.Builder.fromSample(sample)
        .withWebinSubmissionAccountId(webinId)
        .withNoDomain()
        .build();
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

  public Sample handleStructuredDataWebinUserInData(Sample sample, String id) {
    final AtomicBoolean isWebinIdValid = new AtomicBoolean(false);

    sample
        .getData()
        .forEach(
            data -> {
              if (data.getDataType() != null) {
                final String structuredDataWebinId = data.getWebinSubmissionAccountId();

                if (structuredDataWebinId == null)
                  throw new StructuredDataWebinIdMissingException();
              }
            });

    if (sample.hasAccession()) {
      isWebinIdValid.set(checkStructureDataAccessibilityForSubmitter(sample, id));
    } else {
      sample
          .getData()
          .forEach(
              data -> {
                if (data.getDataType() != null) {
                  if (id.equalsIgnoreCase(data.getWebinSubmissionAccountId())) {
                    isWebinIdValid.set(true);
                  }
                }
              });
    }

    if (isWebinIdValid.get()) return sample;
    else throw new StructuredDataNotAccessibleException();
  }

  public boolean checkIfOriginalSampleWebinSubmitter(Sample sample, String id) {
    final AtomicBoolean isWebinIdValid = new AtomicBoolean(false);

    sample
        .getData()
        .forEach(
            data -> {
              if (data.getDataType() != null) {
                final String structuredDataWebinId = data.getWebinSubmissionAccountId();

                if (structuredDataWebinId == null)
                  throw new StructuredDataWebinIdMissingException();
              }
            });

    if (sample.hasAccession()) {
      isWebinIdValid.set(checkStructureDataAccessibilityForSubmitter(sample, id));
    }

    if (isWebinIdValid.get()) return true;
    else throw new StructuredDataNotAccessibleException();
  }

  private boolean checkStructureDataAccessibilityForSubmitter(Sample sample, String id) {
    final AtomicBoolean isWebinIdValid = new AtomicBoolean(false);

    final Optional<Sample> oldSample =
        sampleService.fetch(sample.getAccession(), Optional.empty(), null);

    if (oldSample.isPresent()) {
      Sample oldSampleRetrieved = oldSample.get();

      sample
          .getData()
          .forEach(
              data -> {
                final StructuredDataType dataType = data.getDataType();

                if (dataType != null) {
                  Optional<AbstractData> filteredData =
                      oldSampleRetrieved.getData().stream()
                          .filter(oldSampledata -> oldSampledata.getDataType().equals(dataType))
                          .findFirst();

                  final String webinSubmissionAccountId = data.getWebinSubmissionAccountId();

                  if (filteredData.isPresent()) {
                    AbstractData fData = filteredData.get();

                    if (!webinSubmissionAccountId.equalsIgnoreCase(
                        fData.getWebinSubmissionAccountId())) {
                      throw new StructuredDataNotAccessibleException();
                    } else {
                      isWebinIdValid.set(true);
                    }
                  } else {
                    if (id.equalsIgnoreCase(webinSubmissionAccountId)) {
                      isWebinIdValid.set(true);
                    }
                  }
                }
              });
    } else {
      sample
          .getData()
          .forEach(
              data -> {
                if (data.getDataType() != null) {
                  if (id.equalsIgnoreCase(data.getWebinSubmissionAccountId())) {
                    isWebinIdValid.set(true);
                  }
                }
              });
    }

    return isWebinIdValid.get();
  }

  public boolean isWebinSuperUser(String webinId) {
    return webinId.equalsIgnoreCase(bioSamplesProperties.getBiosamplesClientWebinUsername());
  }

  @ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Unauthorized WEBIN user")
  public static class WebinUserLoginUnauthorizedException extends RuntimeException {}

  @ResponseStatus(
      value = HttpStatus.FORBIDDEN,
      reason =
          "This sample is private and not available for browsing. If you think this is an error and/or you should have access please contact the BioSamples Helpdesk at biosamples@ebi.ac.uk")
  public static class SampleNotAccessibleException extends RuntimeException {}

  @ResponseStatus(
      value = HttpStatus.FORBIDDEN,
      reason =
          "You don't have access to the sample structured data. If you think this is an error and/or you should have access please contact the BioSamples Helpdesk at biosamples@ebi.ac.uk") // 403
  public static class StructuredDataNotAccessibleException extends RuntimeException {}

  @ResponseStatus(
      value = HttpStatus.BAD_REQUEST,
      reason = "Structured data must have a webin submission account id") // 400
  public static class StructuredDataWebinIdMissingException extends RuntimeException {}
}
