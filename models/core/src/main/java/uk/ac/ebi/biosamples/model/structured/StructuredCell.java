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
package uk.ac.ebi.biosamples.model.structured;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StructuredCell implements Comparable<StructuredCell> {
  @JsonProperty("value")
  private String value;

  @JsonProperty("iri")
  private String iri;

  @Override
  public int compareTo(final StructuredCell other) {
    final int cmp = nullSafeStringComparison(getValue(), other.getValue());

    if (cmp != 0) {
      return cmp;
    }

    return nullSafeStringComparison(getIri(), other.getIri());
  }

  private static int nullSafeStringComparison(String str1, String str2) {
    if (str1 == null) {
      return str2 == null ? 0 : -1;
    }

    return str2 == null ? 1 : str1.compareTo(str2);
  }
}
