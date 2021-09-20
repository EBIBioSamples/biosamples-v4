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
package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SampleFacetValue implements Comparable<SampleFacetValue> {
  public final String label;
  public final long count;

  public SampleFacetValue(String label, long count) {
    this.label = label;
    this.count = count;
  }

  @Override
  public int compareTo(SampleFacetValue o) {
    return Long.compare(this.count, o.count);
  }

  @JsonCreator
  public static SampleFacetValue build(
      @JsonProperty("label") String label, @JsonProperty("count") long count) {
    if (label == null || label.trim().length() == 0) {
      throw new IllegalArgumentException("label must not be blank");
    }
    return new SampleFacetValue(label.trim(), count);
  }
}
