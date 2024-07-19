package uk.ac.ebi.biosamples.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionReceipt {
  private List<Sample> samples;
  private List<ErrorReceipt> errors;

  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class ErrorReceipt {
    private String sampleName;
    private String error;
  }
}



