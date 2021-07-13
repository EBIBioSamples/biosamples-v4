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
package uk.ac.ebi.biosamples;

import java.util.ArrayList;
import java.util.List;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

public class PipelineFutureCallback implements ThreadUtils.Callback<PipelineResult> {
  private long totalCount = 0;
  private final List<String> failedSamples = new ArrayList<>();

  public void call(PipelineResult pipelineResult) {
    totalCount = totalCount + pipelineResult.getModifiedRecords();
    if (!pipelineResult.isSuccess()) {
      failedSamples.add(pipelineResult.getAccession());
    }
  }

  public long getTotalCount() {
    return totalCount;
  }

  public List<String> getFailedSamples() {
    return failedSamples;
  }
}
