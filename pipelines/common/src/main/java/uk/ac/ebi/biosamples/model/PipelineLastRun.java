package uk.ac.ebi.biosamples.model;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document
@Jacksonized
@Builder
@Getter
public class PipelineLastRun {
  @Id
  private String id;
  private PipelineName pipelineName;
  private LocalDate lastRunDate;
}
