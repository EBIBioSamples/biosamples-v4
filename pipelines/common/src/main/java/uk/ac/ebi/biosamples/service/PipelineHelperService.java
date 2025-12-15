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
package uk.ac.ebi.biosamples.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.PipelineLastRun;
import uk.ac.ebi.biosamples.model.PipelineName;
import uk.ac.ebi.biosamples.repository.PipelineLastRunRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class PipelineHelperService {
  private final PipelineLastRunRepository pipelineLastRunRepository;

  public PipelineLastRun getLastRunDate(PipelineName pipelineName) {
    return pipelineLastRunRepository
        .findFirstByPipelineName(pipelineName)
        .orElse(
            PipelineLastRun.builder()
                .pipelineName(pipelineName)
                .lastRunDate(LocalDate.EPOCH)
                .build());
  }

  public void updateLastRunDate(PipelineLastRun pipelineLastRun, LocalDate lastRunDate) {
    PipelineLastRun updated =
        PipelineLastRun.builder()
            .id(pipelineLastRun.getId())
            .pipelineName(pipelineLastRun.getPipelineName())
            .lastRunDate(lastRunDate)
            .build();
    pipelineLastRunRepository.save(updated);
  }
}
