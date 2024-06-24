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

import static uk.ac.ebi.biosamples.BioSamplesConstants.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;

@Service
public class BioSamplesCrossSourceIngestAccessControlService {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final String ENA_CHECKLIST = "ENA-CHECKLIST";

  @Autowired BioSamplesProperties bioSamplesProperties;

  /**
   * Checks if a sample update by an AAP user is allowed based on the submitter type. AAP users
   * cannot update samples submitted by WEBIN users.
   *
   * @param oldSample The old sample object.
   * @param sample The new sample object.
   * @throws GlobalExceptions.AccessControlException if the update is not allowed.
   */
  public void checkAndPreventWebinUserSampleUpdateByAapUser(
      final Sample oldSample, final Sample sample) {
    // Check if the old sample was submitted by a WEBIN user
    if (oldSample.getWebinSubmissionAccountId() != null) {
      // If the old sample was submitted by a WEBIN super-user
      if (oldSample
          .getWebinSubmissionAccountId()
          .equalsIgnoreCase(bioSamplesProperties.getBiosamplesClientWebinUsername())) {
        // If the domain is not null and is not "ncbi" (NCBI import domain can update WEBIN
        // super-user samples)
        if (!isNcbiSampleAndNcbiSuperUserDomain(sample)) {
          // Throw an exception as AAP users cannot update WEBIN user samples
          throw new GlobalExceptions.AccessControlException(
              "An AAP submitter cannot update a sample submitted by a WEBIN submitter");
        }
      } else {
        // Throw an exception as AAP users cannot update WEBIN user samples
        throw new GlobalExceptions.AccessControlException(
            "An AAP submitter cannot update a sample submitted by a WEBIN submitter");
      }
    }
  }

  private boolean isNcbiSampleAndNcbiSuperUserDomain(final Sample newSample) {
    return (newSample.getAccession().startsWith(NCBI_ACCESSION_PREFIX)
            || newSample.getAccession().startsWith(DDBJ_ACCESSION_PREFIX))
        && isPipelineNcbiDomain(newSample.getDomain());
  }

  public boolean isPipelineNcbiDomain(final String domain) {
    if (domain == null) {
      return false;
    }

    return domain.equalsIgnoreCase(NCBI_IMPORT_DOMAIN);
  }

  public void isOriginalSubmitterInSampleMetadata(
      final String webinIdInOldSample, final Sample newSample) {
    log.info("Super user and file upload submission");

    // file uploader submission access protection
    if (webinIdInOldSample != null
        && !webinIdInOldSample.equals(newSample.getWebinSubmissionAccountId())) {
      throw new GlobalExceptions.NotOriginalSubmitterException();
    }
  }

  public void accessControlWebinSourcedSampleByCheckingEnaChecklistAttribute(
      final Sample oldSample, final Sample newSample) {
    /*
    Old sample has ENA-CHECKLIST attribute, hence it can be concluded that it is imported from ENA
    New sample has ENA-CHECKLIST attribute, means its updated by ENA pipeline or WEBIN, allow further computation
    New sample doesn't have ENA-CHECKLIST attribute, means it's not updated by ENA pipeline or WEBIN, don't allow further computation and throw exception
     */
    if (oldSample.getAttributes().stream()
        .anyMatch(attribute -> attribute.getType().equalsIgnoreCase(ENA_CHECKLIST))) {
      if (newSample.getAttributes().stream()
          .noneMatch(attribute -> attribute.getType().equalsIgnoreCase(ENA_CHECKLIST))) {
        throw new GlobalExceptions.InvalidSubmissionSourceException();
      }
    }
  }

  public void accessControlWebinSourcedSampleByCheckingSubmittedViaType(
      final Sample oldSample, final Sample newSample) {
    if (oldSample.getSubmittedVia() == SubmittedViaType.WEBIN_SERVICES) {
      if (newSample.getSubmittedVia() != SubmittedViaType.WEBIN_SERVICES) {
        throw new GlobalExceptions.InvalidSubmissionSourceException();
      }
    }
  }

  public void accessControlPipelineImportedSamples(final Sample oldSample, final Sample newSample) {
    if (oldSample.getSubmittedVia()
        == SubmittedViaType.PIPELINE_IMPORT) { // pipeline imports access protection
      if (newSample.getSubmittedVia() != SubmittedViaType.PIPELINE_IMPORT) {
        throw new GlobalExceptions.InvalidSubmissionSourceException();
      }
    }
  }

  public void validateFileUploaderSampleUpdateHasAlwaysExistingAccession(final Sample newSample) {
    if (newSample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
      log.error(
          "Not permitted to update sample not in database using the file uploader, accession: {}",
          newSample.getAccession());

      throw new GlobalExceptions.SampleAccessionDoesNotExistException();
    }
  }

  public void preventAapDomainChangeForFileUploadSampleSubmissions(
      final Sample oldSample, final String newSampleSubmissionDomain) {
    if (!newSampleSubmissionDomain.equals(oldSample.getDomain())) {
      throw new GlobalExceptions.SampleDomainMismatchException();
    }
  }
}
