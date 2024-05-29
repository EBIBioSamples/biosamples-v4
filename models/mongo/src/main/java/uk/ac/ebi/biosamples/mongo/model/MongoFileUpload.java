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
package uk.ac.ebi.biosamples.mongo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.ac.ebi.biosamples.mongo.util.BioSamplesFileUploadSubmissionStatus;
import uk.ac.ebi.biosamples.mongo.util.SampleNameAccessionPair;

@Document(collection = "mongoFileUpload")
@Getter
public class MongoFileUpload {
  @Id @JsonIgnore @Indexed private final String submissionId;
  private final BioSamplesFileUploadSubmissionStatus submissionStatus;
  private final String submissionDate;
  private final String lastUpdateDate;
  private final String submitterDetails;
  private final String checklist;
  private final List<SampleNameAccessionPair> sampleNameAccessionPairs;
  @JsonIgnore private final boolean isWebin;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final String validationMessage;

  public MongoFileUpload(
      final String submissionId,
      final BioSamplesFileUploadSubmissionStatus submissionStatus,
      final String submissionDate,
      final String lastUpdateDate,
      final String submitterDetails,
      final String checklist,
      final boolean isWebin,
      final List<SampleNameAccessionPair> sampleNameAccessionPairs,
      final String validationMessage) {
    this.submissionId = submissionId;
    this.submissionStatus = submissionStatus;
    this.submissionDate = submissionDate;
    this.lastUpdateDate = lastUpdateDate;
    this.submitterDetails = submitterDetails;
    this.checklist = checklist;
    this.isWebin = isWebin;
    this.sampleNameAccessionPairs = sampleNameAccessionPairs;
    this.validationMessage = validationMessage;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MongoFileUpload)) {
      return false;
    }
    final MongoFileUpload that = (MongoFileUpload) o;

    return isWebin() == that.isWebin()
        && Objects.equals(getSubmissionId(), that.getSubmissionId())
        && getSubmissionStatus() == that.getSubmissionStatus()
        && Objects.equals(getSubmitterDetails(), that.getSubmitterDetails())
        && Objects.equals(getChecklist(), that.getChecklist())
        && Objects.equals(getSampleNameAccessionPairs(), that.getSampleNameAccessionPairs())
        && Objects.equals(getValidationMessage(), that.getValidationMessage());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getSubmissionId(),
        getSubmissionStatus(),
        getSubmitterDetails(),
        getChecklist(),
        isWebin(),
        getSampleNameAccessionPairs(),
        getValidationMessage());
  }
}
