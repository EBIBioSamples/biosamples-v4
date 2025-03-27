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

import java.util.Date;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.ac.ebi.biosamples.mongo.util.PipelineCompletionStatus;

@Document(collection = "pipelineRunStatus")
public class MongoPipeline {
  @Id @Indexed private final String pipelineUniqueIdentifier;
  private final Date pipelineRunDate;
  private final String pipelineName;
  private final PipelineCompletionStatus pipelineCompletionStatus;
  private final String failedSamples;
  private final String exceptionCause;

  public MongoPipeline(
      final String pipelineUniqueIdentifier,
      final Date pipelineRunDate,
      final String pipelineName,
      final PipelineCompletionStatus pipelineCompletionStatus,
      final String failedSamples,
      final String exceptionCause) {
    this.pipelineUniqueIdentifier = pipelineUniqueIdentifier;
    this.pipelineRunDate = pipelineRunDate;
    this.pipelineName = pipelineName;
    this.pipelineCompletionStatus = pipelineCompletionStatus;
    this.failedSamples = failedSamples;
    this.exceptionCause = exceptionCause;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MongoPipeline)) {
      return false;
    }
    final MongoPipeline that = (MongoPipeline) o;
    return Objects.equals(pipelineUniqueIdentifier, that.pipelineUniqueIdentifier)
        && Objects.equals(pipelineRunDate, that.pipelineRunDate)
        && Objects.equals(pipelineName, that.pipelineName)
        && pipelineCompletionStatus == that.pipelineCompletionStatus;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        pipelineUniqueIdentifier, pipelineRunDate, pipelineName, pipelineCompletionStatus);
  }

  @Override
  public String toString() {
    return "MongoPipeline{"
        + "pipelineUniqueIdentifier='"
        + pipelineUniqueIdentifier
        + '\''
        + ", pipelineRunDate="
        + pipelineRunDate
        + ", pipelineName='"
        + pipelineName
        + '\''
        + ", pipelineCompletionStatus="
        + pipelineCompletionStatus
        + '}';
  }
}
