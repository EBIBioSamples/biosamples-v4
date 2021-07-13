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

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.ac.ebi.biosamples.mongo.util.BioSamplesFileUploadSubmissionStatus;

@Document(collection = "mongoFileUpload")
public class MongoFileUpload {
  @Id private final String xmlPayloadId;
  private final BioSamplesFileUploadSubmissionStatus submissionStatus;
  private final String submitterDetails;
  private final String checklist;
  private final boolean isWebin;
  private List<String> accessions;

  public MongoFileUpload(
      String xmlPayloadId,
      BioSamplesFileUploadSubmissionStatus submissionStatus,
      String submitterDetails,
      String checklist,
      boolean isWebin,
      List<String> accessions) {
    this.xmlPayloadId = xmlPayloadId;
    this.submissionStatus = submissionStatus;
    this.submitterDetails = submitterDetails;
    this.checklist = checklist;
    this.isWebin = isWebin;
    this.accessions = accessions;
  }

  public String getChecklist() {
    return checklist;
  }

  public String getXmlPayloadId() {
    return xmlPayloadId;
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

  public List<String> getAccessions() {
    return accessions;
  }
}
