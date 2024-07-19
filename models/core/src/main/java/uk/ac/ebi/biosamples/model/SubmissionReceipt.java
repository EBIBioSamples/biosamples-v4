package uk.ac.ebi.biosamples.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SubmissionReceipt {
  private final List<Sample> samples;
  private final List<ErrorReceipt> errors;

  @AllArgsConstructor
  @Getter
  public static class ErrorReceipt {
    private final String sampleName;
    private final String error;
  }
}



