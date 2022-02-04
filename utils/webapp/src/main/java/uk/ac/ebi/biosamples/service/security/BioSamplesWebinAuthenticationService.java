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
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;
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

  public SubmissionAccount getWebinSubmissionAccount(final HttpServletRequest request) {
    final Authentication authentication = bearerTokenExtractor.extract(request);

    if (authentication != null) {
      return getWebinSubmissionAccount(String.valueOf(authentication.getPrincipal())).getBody();
    }

    return null;
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
    final String webinSubmissionAccountIdInMetadata = sample.getWebinSubmissionAccountId();

    if (webinId != null && !webinId.isEmpty()) { // webin id retrieval failure - throw Exception
      final String webinIdToUse =
          (webinSubmissionAccountIdInMetadata != null
                  && !webinSubmissionAccountIdInMetadata.isEmpty())
              ? webinSubmissionAccountIdInMetadata
              : biosamplesClientWebinUsername;

      if (sample.getAccession() != null) { // sample updates, where sample has an accession
        final Optional<Sample> oldSample = getOldSample(sample);

        if (webinId.equalsIgnoreCase(
            biosamplesClientWebinUsername)) { // ENA pipeline submissions or super user submission
          // (via FILE UPLOADER)
          if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
            if (oldSample.isPresent()
                && !sample
                    .getWebinSubmissionAccountId()
                    .equals(oldSample.get().getWebinSubmissionAccountId())) {
              throw new SampleNotAccessibleException();
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
                return getSampleWithWebinSubmissionAccountId(sample, webinIdToUse);
              } else {
                return getSampleWithWebinSubmissionAccountId(
                    sample, oldSavedSampleWebinSubmissionAccountId);
              }
            } else {
              final String oldSampleDomain = oldSavedSample.getDomain();

              if (sampleService.isPipelineEnaOrNcbiDomain(oldSampleDomain)
                  || checkOtherENARegistrationDomains(
                      oldSampleDomain)) { // if old sample was a pipeline submission using AAP, or
                // pre registration, allow
                // webin replacement
                return getSampleWithWebinSubmissionAccountId(sample, webinIdToUse);
              } else {
                throw new SampleNotAccessibleException();
              }
            }
          } else {
            return getSampleWithWebinSubmissionAccountId(sample, webinIdToUse);
          }
        } else { // normal sample update - not pipeline, check for old user, if mismatch throw
          // exception, else build the Sample
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
            biosamplesClientWebinUsername)) { // new submission by client program
          return getSampleWithWebinSubmissionAccountId(sample, webinIdToUse);
        } else {
          return getSampleWithWebinSubmissionAccountId(sample, webinId);
        }
      }
    } else {
      throw new WebinUserLoginUnauthorizedException();
    }
  }

  /*Only used for sample migration purposes*/
  private boolean checkOtherENARegistrationDomains(final String sampleDomain) {
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

  private Optional<Sample> getOldSample(final Sample sample) {
    return sampleService.fetch(sample.getAccession(), Optional.empty(), null);
  }

  public Sample getSampleWithWebinSubmissionAccountId(final Sample sample, final String webinId) {
    return Sample.Builder.fromSample(sample)
        .withWebinSubmissionAccountId(webinId)
        .withNoDomain()
        .build();
  }

  public CurationLink handleWebinUser(final CurationLink curationLink, final String webinId) {
    if (webinId != null && !webinId.isEmpty()) {
      return CurationLink.build(
          curationLink.getSample(),
          curationLink.getCuration(),
          null,
          webinId,
          curationLink.getCreated());
    } else {
      throw new SampleNotAccessibleException();
    }
  }

  public void handleStructuredDataAccesibility(
      final StructuredData structuredData, final String id) {
    structuredData
        .getData()
        .forEach(
            data -> {
              if (data.getWebinSubmissionAccountId() == null
                  || data.getWebinSubmissionAccountId().isEmpty()) {
                throw new StructuredDataWebinIdMissingException();
              } else if (!id.equalsIgnoreCase(data.getWebinSubmissionAccountId())) {
                throw new WebinUserLoginUnauthorizedException();
              }
            });
  }

  public boolean isSampleOwner(final Sample sample, final String id) {
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
      isWebinIdValid.set(checkStructureDataAccessibility(sample, id));
    }

    if (isWebinIdValid.get()) return true;
    else throw new StructuredDataNotAccessibleException();
  }

  private boolean checkStructureDataAccessibility(final Sample sample, final String webinId) {
    final AtomicBoolean isWebinIdValid = new AtomicBoolean(false);
    final Optional<Sample> oldSample = getOldSample(sample);

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

  public void checkSampleAccessibility(
      final Sample sample, final SubmissionAccount webinSubmissionAccount) {
    if (webinSubmissionAccount == null) {
      if (sample.getRelease().isBefore(Instant.now())) {
        // release date in past, accessible
      } else {
        throw new SampleNotAccessibleException();
      }
    } else {
      if (sample.getRelease().isBefore(Instant.now())) {
        // release date in past, accessible
      } else {
        final String webinSubmissionAccountId = sample.getWebinSubmissionAccountId();

        if (webinSubmissionAccountId == null) {
          throw new SampleNotAccessibleException();
        } else if (webinSubmissionAccountId.equals(webinSubmissionAccount.getId())) {
          // if the current user is the submitter of the original sample, accessible
        } else {
          throw new SampleNotAccessibleException();
        }
      }
    }
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

  @ResponseStatus(
      value = HttpStatus.BAD_REQUEST,
      reason = "You must provide a bearer token to be able to submit") // 400
  public static class WebinTokenMissingException extends RuntimeException {}
}
