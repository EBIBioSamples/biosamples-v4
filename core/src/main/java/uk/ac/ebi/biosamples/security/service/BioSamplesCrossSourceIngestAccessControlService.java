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
package uk.ac.ebi.biosamples.security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.SubmittedViaType;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;

@Service
public class BioSamplesCrossSourceIngestAccessControlService {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final String ENA_CHECKLIST = "ENA-CHECKLIST";

  @Autowired BioSamplesProperties bioSamplesProperties;

  public void isOriginalSubmitterInSampleMetadata(
      final String webinIdInOldSample, final Sample newSample) {
    log.info("Super user and file upload submission");

    // file uploader submission access protection
    if (webinIdInOldSample != null
        && !webinIdInOldSample.equals(newSample.getWebinSubmissionAccountId())) {
      throw new GlobalExceptions.NonSubmitterUpdateAttemptException();
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

  public void validateFileUploaderSampleUpdateHasExistingAccession(final Sample newSample) {
    if (newSample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
      log.error(
          "Not permitted to update sample not in database using the file uploader, accession: {}",
          newSample.getAccession());

      throw new GlobalExceptions.SampleAccessionDoesNotExistException();
    }
  }
}
