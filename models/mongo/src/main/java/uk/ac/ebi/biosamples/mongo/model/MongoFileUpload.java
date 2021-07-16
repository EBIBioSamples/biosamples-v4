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
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.ac.ebi.biosamples.mongo.util.BioSamplesFileUploadSubmissionStatus;
import uk.ac.ebi.biosamples.mongo.util.SampleNameAccessionPair;

@Document(collection = "mongoFileUpload")
public class MongoFileUpload {
  @Id @JsonIgnore @Indexed private final String submissionId;
  private final BioSamplesFileUploadSubmissionStatus submissionStatus;
  private final String submitterDetails;
  private final String checklist;
  @JsonIgnore private final boolean isWebin;
  private final List<SampleNameAccessionPair> nameAccessionPairs;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final String validationMessage;

  public MongoFileUpload(
      final String submissionId,
      final BioSamplesFileUploadSubmissionStatus submissionStatus,
      final String submitterDetails,
      final String checklist,
      final boolean isWebin,
      final List<SampleNameAccessionPair> nameAccessionPairs,
      String validationMessage) {
    this.submissionId = submissionId;
    this.submissionStatus = submissionStatus;
    this.submitterDetails = submitterDetails;
    this.checklist = checklist;
    this.isWebin = isWebin;
    this.nameAccessionPairs = nameAccessionPairs;
    this.validationMessage = validationMessage;
  }

  public String getChecklist() {
    return checklist;
  }

  public String getSubmissionId() {
    return submissionId;
  }

  public BioSamplesFileUploadSubmissionStatus getSubmissionStatus() {
    return submissionStatus;
  }

  public String getSubmitterDetails() {
    return submitterDetails;
  }

  public boolean isWebin() {
    return isWebin;
  }

  public List<SampleNameAccessionPair> getNameAccessionPairs() {
    return nameAccessionPairs;
  }

  public String getValidationMessage() {
    return validationMessage;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof MongoFileUpload)) return false;
    final MongoFileUpload that = (MongoFileUpload) o;

    return isWebin() == that.isWebin()
        && Objects.equals(getSubmissionId(), that.getSubmissionId())
        && getSubmissionStatus() == that.getSubmissionStatus()
        && Objects.equals(getSubmitterDetails(), that.getSubmitterDetails())
        && Objects.equals(getChecklist(), that.getChecklist())
        && Objects.equals(getNameAccessionPairs(), that.getNameAccessionPairs())
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
        getNameAccessionPairs(),
        getValidationMessage());
  }
}
