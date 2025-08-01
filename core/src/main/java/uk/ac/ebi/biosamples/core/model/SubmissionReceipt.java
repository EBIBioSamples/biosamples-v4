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
package uk.ac.ebi.biosamples.core.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private List<ValidationError> errors;
  }

  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class ValidationError {
    private String dataPath;
    private List<String> errors;
  }
}
