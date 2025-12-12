package uk.ac.ebi.biosamples.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.PipelineLastRun;
import uk.ac.ebi.biosamples.model.PipelineName;
import uk.ac.ebi.biosamples.repository.PipelineLastRunRepository;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class PipelineHelperService {
  private final PipelineLastRunRepository pipelineLastRunRepository;

  public LocalDate getLastRunDate(PipelineName pipelineName) {
    PipelineLastRun pipelineLastRun = pipelineLastRunRepository.findFirstByPipelineName(pipelineName)
        .orElse(PipelineLastRun.builder().pipelineName(pipelineName).lastRunDate(LocalDate.EPOCH).build());
    return pipelineLastRun.getLastRunDate();
  }
}
