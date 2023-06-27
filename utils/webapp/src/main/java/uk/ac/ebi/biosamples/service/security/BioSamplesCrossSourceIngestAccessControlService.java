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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;

@Service
public class BioSamplesCrossSourceIngestAccessControlService {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final String ENA_CHECKLIST = "ENA-CHECKLIST";

  public void protectFileUploaderSampleAccess(final Sample oldSample, final Sample sample) {
    if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
      log.info("Super user and file upload submission");

      // file uploader submission access protection
      if (!oldSample.getWebinSubmissionAccountId().equals(sample.getWebinSubmissionAccountId())) {
        throw new GlobalExceptions.SampleNotAccessibleException();
      }
    }
  }

  public void protectPipelineImportedAndFileUploaderSubmittedSampleAccess(
      final Sample oldSample, final Sample sample) {
    if (oldSample.getSubmittedVia()
        == SubmittedViaType.PIPELINE_IMPORT) { // pipeline imports access protection
      if (sample.getSubmittedVia() != SubmittedViaType.PIPELINE_IMPORT) {
        throw new GlobalExceptions.InvalidSubmissionSourceException();
      }
    }

    if (oldSample.getSubmittedVia()
        == SubmittedViaType.FILE_UPLOADER) { // file uploader samples re-update protection
      if (sample.getSubmittedVia() != SubmittedViaType.FILE_UPLOADER) {
        // file uploader submission access protection
        if (!oldSample.getWebinSubmissionAccountId().equals(sample.getWebinSubmissionAccountId())) {
          throw new GlobalExceptions.SampleNotAccessibleException();
        }
      }
    }
  }

  public void protectEnaPipelineImportedSampleAccess(final Sample oldSample, final Sample sample) {
    /*
    Old sample has ENA-CHECKLIST attribute, hence it can be concluded that it is imported from ENA
    New sample has ENA-CHECKLIST attribute, means its updated by ENA pipeline, allow further computation
    New sample doesn't have ENA-CHECKLIST attribute, means it's not updated by ENA pipeline, don't allow further computation and throw exception
     */
    if (oldSample.getAttributes().stream()
        .anyMatch(attribute -> attribute.getType().equalsIgnoreCase(ENA_CHECKLIST))) {
      if (sample.getAttributes().stream()
          .noneMatch(attribute -> attribute.getType().equalsIgnoreCase(ENA_CHECKLIST))) {
        throw new GlobalExceptions.InvalidSubmissionSourceException();
      }
    }
  }

  public void protectPipelineImportedSampleAccess(final Sample oldSample, final Sample sample) {
    if (oldSample.getSubmittedVia()
        == SubmittedViaType.PIPELINE_IMPORT) { // pipeline imports access protection
      if (sample.getSubmittedVia() != SubmittedViaType.PIPELINE_IMPORT) {
        throw new GlobalExceptions.InvalidSubmissionSourceException();
      }
    }
  }

  public void validateFileUploaderSampleAccessionWhileSampleUpdate(final Sample sample) {
    if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
      log.error(
          "Not permitted to update sample not in database using the file uploader, accession: {}",
          sample.getAccession());

      throw new GlobalExceptions.SampleAccessionDoesNotExistException();
    }
  }

  public void protectFileUploaderAapSample(
      final Sample oldSample, final Sample sample, final String domain) {
    if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
      if (!domain.equals(oldSample.getDomain())) {
        throw new GlobalExceptions.SampleDomainMismatchException();
      }
    }
  }
}
