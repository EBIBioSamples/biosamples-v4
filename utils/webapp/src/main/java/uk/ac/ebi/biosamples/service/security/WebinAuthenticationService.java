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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataType;
import uk.ac.ebi.biosamples.service.SampleService;

@Service
public class WebinAuthenticationService {
  public static final String ATLANTICO_DOMAIN = "self.AtlantECO";
  private final RestTemplate restTemplate;
  private final SampleService sampleService;
  private final BioSamplesCrossSourceIngestAccessControlService
      bioSamplesCrossSourceIngestAccessControlService;
  private final BioSamplesProperties bioSamplesProperties;

  public WebinAuthenticationService(
      final SampleService sampleService,
      final BioSamplesCrossSourceIngestAccessControlService
          bioSamplesCrossSourceIngestAccessControlService,
      final BioSamplesProperties bioSamplesProperties) {
    this.restTemplate = new RestTemplate();
    this.sampleService = sampleService;
    this.bioSamplesCrossSourceIngestAccessControlService =
        bioSamplesCrossSourceIngestAccessControlService;
    this.bioSamplesProperties = bioSamplesProperties;
  }

  public Sample handleWebinUserSubmission(
      final Sample sample, final String principle, final Optional<Sample> oldSample) {
    final String proxyWebinId = bioSamplesProperties.getBiosamplesClientWebinUsername();
    final String webinIdInSample = sample.getWebinSubmissionAccountId();

    if (principle != null && !principle.isEmpty()) {
      final String webinIdToSetForSample =
          (webinIdInSample != null && !webinIdInSample.isEmpty()) ? webinIdInSample : proxyWebinId;

      if (sample.getAccession() != null) {
        return handleSampleUpdate(
            sample, oldSample, principle, proxyWebinId, webinIdToSetForSample);
      } else {
        return handleNewSubmission(sample, principle, proxyWebinId, webinIdToSetForSample);
      }
    } else {
      throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
    }
  }

  private Sample handleSampleUpdate(
      final Sample sample,
      final Optional<Sample> oldSample,
      final String webinIdFromAuthToken,
      final String proxyWebinId,
      final String webinIdToSetForSample) {
    if (webinIdFromAuthToken.equalsIgnoreCase(proxyWebinId)) {
      return handleWebinSuperUserSampleSubmission(sample, oldSample, webinIdToSetForSample);
    } else {
      return handleNormalSampleUpdate(sample, oldSample, webinIdFromAuthToken);
    }
  }

  private Sample handleNewSubmission(
      final Sample sample,
      final String webinIdFromAuthToken,
      final String proxyWebinId,
      final String webinIdToSetForSample) {
    if (webinIdFromAuthToken.equalsIgnoreCase(proxyWebinId)) {
      return buildSampleWithWebinId(sample, webinIdToSetForSample);
    } else {
      return buildSampleWithWebinId(sample, webinIdFromAuthToken);
    }
  }

  private Sample handleNormalSampleUpdate(
      final Sample sample, final Optional<Sample> oldSample, final String webinIdFromAuthToken) {
    if (oldSample.isPresent()) {
      final Sample oldSampleInDb = oldSample.get();

      existingSampleAccessibilityChecks(sample, oldSampleInDb);

      if (!webinIdFromAuthToken.equalsIgnoreCase(oldSampleInDb.getWebinSubmissionAccountId())) {
        throw new GlobalExceptions.NonSubmitterUpdateAttemptException();
      } else {
        return buildSampleWithWebinId(sample, webinIdFromAuthToken);
      }
    } else {
      throw new GlobalExceptions.SampleAccessionDoesNotExistException();
    }
  }

  private void existingSampleAccessibilityChecks(final Sample sample, final Sample oldSampleInDb) {
    bioSamplesCrossSourceIngestAccessControlService.accessControlPipelineImportedSamples(
        oldSampleInDb, sample);
    bioSamplesCrossSourceIngestAccessControlService
        .accessControlWebinSourcedSampleByCheckingEnaChecklistAttribute(oldSampleInDb, sample);
    bioSamplesCrossSourceIngestAccessControlService
        .accessControlWebinSourcedSampleByCheckingSubmittedViaType(oldSampleInDb, sample);
  }

  private Sample handleWebinSuperUserSampleSubmission(
      final Sample sample, final Optional<Sample> oldSample, final String webinIdToSetForSample) {
    if (oldSample.isPresent()) {
      final Sample oldSampleInDb = oldSample.get();
      final String webinIdInOldSample = oldSampleInDb.getWebinSubmissionAccountId();

      if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
        fileUploaderSampleSubmissionAccessibilityChecks(sample, oldSampleInDb, webinIdInOldSample);
      }

      if (webinIdInOldSample != null && !webinIdInOldSample.isEmpty()) {
        return buildSampleWithWebinId(sample, webinIdToSetForSample);
      } else {
        final String oldSampleAapDomain = oldSampleInDb.getDomain();

        if (sampleService.isPipelineNcbiDomain(oldSampleAapDomain)) {
          return sample;
        } else {
          return buildSampleWithWebinId(sample, webinIdToSetForSample);
        }
      }
    } else {
      return buildSampleWithWebinId(sample, webinIdToSetForSample);
    }
  }

  private boolean isOwnershipChangeFromAapToWebinAllowed() {
    return true;
  }

  private boolean isSameDomain(final String domain, final String oldSampleAapDomain) {
    return domain != null && domain.equalsIgnoreCase(oldSampleAapDomain);
  }

  private void fileUploaderSampleSubmissionAccessibilityChecks(
      final Sample sample, final Sample oldSampleInDb, final String webinIdInOldSample) {
    bioSamplesCrossSourceIngestAccessControlService.isOriginalSubmitterInSampleMetadata(
        webinIdInOldSample, sample);
    existingSampleAccessibilityChecks(sample, oldSampleInDb);
  }

  /*Only used for sample migration purposes*/
  private boolean isOldRegistrationDomain(final String sampleDomain) {
    if (sampleDomain != null) {
      return sampleDomain.equals("self.Webin")
          || sampleDomain.equals("3fa5e19ccafc88187d437f92cf29c3b6694c6c6f98efa236c8aa0aeaf5b23f15")
          || sampleDomain.equals("self.BiosampleImportAcccession")
          || sampleDomain.equals("self.BioSamplesMigration")
          || sampleDomain.startsWith("self.BioSamples")
          || sampleDomain.startsWith("self.BiosampleSyntheticData");
    } else {
      return false;
    }
  }

  public Sample buildSampleWithWebinId(final Sample sample, final String webinId) {
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

  public void isStructuredDataAccessible(
      final StructuredData structuredData, final String webinId) {
    structuredData
        .getData()
        .forEach(
            data -> {
              final String structuredDataSubmittersWebinId = data.getWebinSubmissionAccountId();

              if (structuredDataSubmittersWebinId == null
                  || structuredDataSubmittersWebinId.isEmpty()) {
                throw new GlobalExceptions.StructuredDataWebinIdMissingException();
              } else if (!webinId.equalsIgnoreCase(structuredDataSubmittersWebinId)) {
                throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
              }
            });
  }

  public boolean isSampleSubmitter(final Sample sample, final String webinId) {
    final AtomicBoolean isValidWebinSubmitter = new AtomicBoolean(false);

    sample
        .getData()
        .forEach(
            data -> {
              if (data.getDataType() != null) {
                final String structuredDataSubmittersWebinId = data.getWebinSubmissionAccountId();

                if (structuredDataSubmittersWebinId == null) {
                  throw new GlobalExceptions.StructuredDataWebinIdMissingException();
                }
              }
            });

    if (sample.hasAccession()) {
      isValidWebinSubmitter.set(isStructuredDataAccessible(sample, webinId));
    }

    if (isValidWebinSubmitter.get()) {
      return true;
    } else {
      throw new GlobalExceptions.StructuredDataNotAccessibleException();
    }
  }

  private boolean isStructuredDataAccessible(final Sample sample, final String webinId) {
    final AtomicBoolean isWebinIdValid = new AtomicBoolean(false);

    // fetch returns sample with curations applied
    final Optional<Sample> oldSample = sampleService.fetch(sample.getAccession(), true);

    if (oldSample.isPresent()) {
      final Sample oldSampleInDb = oldSample.get();

      sample
          .getData()
          .forEach(
              data -> {
                final StructuredDataType dataType = data.getDataType();

                if (dataType != null) {
                  final Optional<AbstractData> filteredData =
                      oldSampleInDb.getData().stream()
                          .filter(
                              oldSampleStructuredData ->
                                  oldSampleStructuredData.getDataType().equals(dataType))
                          .findFirst();

                  final String structuredDataSubmittersWebinId = data.getWebinSubmissionAccountId();

                  if (filteredData.isPresent()) {
                    final AbstractData fData = filteredData.get();

                    if (!structuredDataSubmittersWebinId.equalsIgnoreCase(
                        fData.getWebinSubmissionAccountId())) {
                      throw new GlobalExceptions.StructuredDataNotAccessibleException();
                    } else {
                      isWebinIdValid.set(true);
                    }
                  } else {
                    if (webinId.equalsIgnoreCase(structuredDataSubmittersWebinId)) {
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

  public void isSampleAccessible(final Sample sample, final String webinId) {
    if (webinId == null) {
      if (!sample.getRelease().isBefore(Instant.now())) {
        throw new GlobalExceptions.SampleNotAccessibleException();
      }
    } else {
      if (!sample.getRelease().isBefore(Instant.now())) {
        if (!isWebinSuperUser(webinId)) {
          if (!webinId.equals(sample.getWebinSubmissionAccountId())) {
            throw new GlobalExceptions.SampleNotAccessibleException();
          }
        }
      }
    }
  }
}
