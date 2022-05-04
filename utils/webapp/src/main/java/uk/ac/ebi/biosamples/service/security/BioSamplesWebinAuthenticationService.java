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

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataType;
import uk.ac.ebi.biosamples.service.SampleService;

@Service
public class BioSamplesWebinAuthenticationService {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final RestTemplate restTemplate;
  private final SampleService sampleService;
  private final BioSamplesProperties bioSamplesProperties;
  private final BearerTokenExtractor bearerTokenExtractor;

  public BioSamplesWebinAuthenticationService(
      final SampleService sampleService, final BioSamplesProperties bioSamplesProperties) {
    this.restTemplate = new RestTemplate();
    this.sampleService = sampleService;
    this.bioSamplesProperties = bioSamplesProperties;
    this.bearerTokenExtractor = new BearerTokenExtractor();
  }

  public ResponseEntity<SubmissionAccount> getWebinSubmissionAccount(final String webinAuthToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + webinAuthToken);
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
      throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
    }
  }

  public ResponseEntity<String> getWebinAuthenticationToken(final String authRequest) {
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
      throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
    }
  }

  public Sample handleWebinUserSubmission(Sample sample, String webinId) {
    final String biosamplesClientWebinUsername =
        bioSamplesProperties.getBiosamplesClientWebinUsername();
    final String webinSubmissionAccountIdInMetadata = sample.getWebinSubmissionAccountId();

    if (webinId != null && !webinId.isEmpty()) { // webin id retrieval failure - throw Exception
      final String webinIdToUse =
          (webinSubmissionAccountIdInMetadata != null
                  && !webinSubmissionAccountIdInMetadata.isEmpty())
              ? webinSubmissionAccountIdInMetadata
              : biosamplesClientWebinUsername;

      if (sample.getAccession() != null) { // sample updates, where sample has an accession
        final Optional<Sample> oldSample = fetchOldSample(sample);

        if (webinId.equalsIgnoreCase(
            biosamplesClientWebinUsername)) { // ENA pipeline submissions or super user submission
          // (via FILE UPLOADER)
          if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
            if (oldSample.isPresent()
                && !sample
                    .getWebinSubmissionAccountId()
                    .equals(oldSample.get().getWebinSubmissionAccountId())) {
              throw new GlobalExceptions.SampleNotAccessibleException();
            }
          }

          if (oldSample.isPresent()) {
            final Sample oldSavedSample = oldSample.get();
            final String oldSavedSampleWebinSubmissionAccountId =
                oldSavedSample.getWebinSubmissionAccountId();

            if (oldSavedSampleWebinSubmissionAccountId != null
                && !oldSavedSampleWebinSubmissionAccountId
                    .isEmpty()) { // if old sample has user info, use it
              if (oldSavedSampleWebinSubmissionAccountId.equals(biosamplesClientWebinUsername)) {
                return buildSampleWithWebinSubmissionAccountId(sample, webinIdToUse);
              } else {
                return buildSampleWithWebinSubmissionAccountId(
                    sample, oldSavedSampleWebinSubmissionAccountId);
              }
            } else {
              final String oldSampleDomain = oldSavedSample.getDomain();

              if (sampleService.isAnImportAapDomain(oldSampleDomain)
                  || checkIfAnyENACurrentOrOldRegistrationDomain(
                      oldSampleDomain)) { // if old sample was a pipeline submission using AAP, or
                // pre registration, allow
                // webin replacement
                return buildSampleWithWebinSubmissionAccountId(sample, webinIdToUse);
              } else {
                throw new GlobalExceptions.SampleNotAccessibleException();
              }
            }
          } else {
            return buildSampleWithWebinSubmissionAccountId(sample, webinIdToUse);
          }
        } else { // normal sample update - not pipeline, check for old user, if mismatch throw
          // exception, else build the Sample
          if (oldSample.isPresent()) {
            final Sample oldSavedSample = oldSample.get();

            if (!webinId.equalsIgnoreCase(
                oldSavedSample.getWebinSubmissionAccountId())) { // original submitter mismatch
              throw new GlobalExceptions.SampleNotAccessibleException();
            } else {
              return buildSampleWithWebinSubmissionAccountId(sample, webinId);
            }
          } else {
            return buildSampleWithWebinSubmissionAccountId(sample, webinId);
          }
        }
      } else { // new submission
        if (webinId.equalsIgnoreCase(
            biosamplesClientWebinUsername)) { // new submission by client program
          return buildSampleWithWebinSubmissionAccountId(sample, webinIdToUse);
        } else {
          return buildSampleWithWebinSubmissionAccountId(sample, webinId);
        }
      }
    } else {
      throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
    }
  }

  /*Only used for sample migration purposes*/
  private boolean checkIfAnyENACurrentOrOldRegistrationDomain(final String sampleDomain) {
    if (sampleDomain != null) {
      return sampleDomain.equals("self.Webin")
          || sampleDomain.equals("3fa5e19ccafc88187d437f92cf29c3b6694c6c6f98efa236c8aa0aeaf5b23f15")
          || sampleDomain.equals("self.BiosampleImportAcccession")
          || sampleDomain.equals("self.BioSamplesMigration")
          || sampleDomain.startsWith("self.BioSamples");
    } else {
      return false;
    }
  }

  private Optional<Sample> fetchOldSample(final Sample sample) {
    return sampleService.fetch(sample.getAccession(), Optional.empty(), null);
  }

  public Sample buildSampleWithWebinSubmissionAccountId(final Sample sample, final String webinId) {
    return Sample.Builder.fromSample(sample)
        .withWebinSubmissionAccountId(webinId)
        .withNoDomain()
        .build();
  }

  public CurationLink handleWebinUserSubmission(
      final CurationLink curationLink, final String webinId) {
    if (webinId != null && !webinId.isEmpty()) {
      return CurationLink.build(
          curationLink.getSample(),
          curationLink.getCuration(),
          null,
          webinId,
          curationLink.getCreated());
    } else {
      throw new GlobalExceptions.SampleNotAccessibleException();
    }
  }

  public void handleStructuredDataAccessibilityForOnlyStructuredDataSubmission(
      final StructuredData structuredData, final String id) {
    log.info("Webin id here is ** -> " + id);

    structuredData
        .getData()
        .forEach(
            data -> {
              final String webinSubmissionAccountId = data.getWebinSubmissionAccountId();
              log.info("Webin id here is -> " + webinSubmissionAccountId);

              if (webinSubmissionAccountId == null || webinSubmissionAccountId.isEmpty()) {
                throw new GlobalExceptions.StructuredDataWebinIdMissingException();
              } else if (!id.equalsIgnoreCase(webinSubmissionAccountId)) {
                throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
              }
            });
  }

  public boolean isStructuredDataSubmittedBySampleSubmitter(final Sample sample, final String id) {
    final AtomicBoolean isWebinIdValid = new AtomicBoolean(false);

    sample
        .getData()
        .forEach(
            data -> {
              if (data.getDataType() != null) {
                final String structuredDataWebinId = data.getWebinSubmissionAccountId();

                if (structuredDataWebinId == null)
                  throw new GlobalExceptions.StructuredDataWebinIdMissingException();
              }
            });

    if (sample.hasAccession()) {
      isWebinIdValid.set(isStructuredDataAccessibleBySubmitter(sample, id));
    }

    if (isWebinIdValid.get()) return true;
    else throw new GlobalExceptions.StructuredDataNotAccessibleException();
  }

  private boolean isStructuredDataAccessibleBySubmitter(final Sample sample, final String webinId) {
    final AtomicBoolean isWebinIdValid = new AtomicBoolean(false);
    final Optional<Sample> oldSample = fetchOldSample(sample);

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
                      throw new GlobalExceptions.StructuredDataNotAccessibleException();
                    } else {
                      isWebinIdValid.set(true);
                    }
                  } else {
                    if (webinId.equalsIgnoreCase(webinSubmissionAccountId)) {
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
                  if (webinId.equalsIgnoreCase(data.getWebinSubmissionAccountId())) {
                    isWebinIdValid.set(true);
                  }
                }
              });
    }

    return isWebinIdValid.get();
  }

  public boolean isWebinSuperUser(final String webinId) {
    return webinId != null
        && webinId.equalsIgnoreCase(bioSamplesProperties.getBiosamplesClientWebinUsername());
  }

  public void checkSampleAccessibility(final Sample sample, final String webinSubmissionAccountId) {
    if (webinSubmissionAccountId == null) {
      if (!sample.getRelease().isBefore(Instant.now())) {
        throw new GlobalExceptions.SampleNotAccessibleException();
      }
    } else {
      if (!sample.getRelease().isBefore(Instant.now())) {
        if (!webinSubmissionAccountId.equals(sample.getWebinSubmissionAccountId())) {
          throw new GlobalExceptions.SampleNotAccessibleException();
        }
      }
    }
  }
}
