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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ValidationReport {
  @JsonProperty("missing_fields")
  private List<String> missingFields = new ArrayList<>();

  @JsonProperty("invalid_types")
  private List<String> invalidTypes = new ArrayList<>();

  @JsonProperty("unknown_fields")
  private List<String> unknownFields = new ArrayList<>();

  public List<String> getMissingFields() {
    return missingFields;
  }

  public List<String> getInvalidTypes() {
    return invalidTypes;
  }

  public List<String> getUnknownFields() {
    return unknownFields;
  }

  public void addMissingField(String field) {
    missingFields.add(field);
  }

  public void addInvalidType(String field) {
    invalidTypes.add(field);
  }

  public void addUnknownField(String field) {
    unknownFields.add(field);
  }

  public boolean isEmpty() {
    return missingFields.isEmpty() && invalidTypes.isEmpty() && unknownFields.isEmpty();
  }
}
