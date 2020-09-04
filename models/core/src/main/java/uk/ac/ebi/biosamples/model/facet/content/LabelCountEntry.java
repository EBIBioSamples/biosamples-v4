/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.model.facet.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Map;

public class LabelCountEntry implements Comparable<LabelCountEntry> {
  private final String label;
  private final long count;

  private LabelCountEntry(String label, long count) {
    this.label = label;
    this.count = count;
  }

  public String getLabel() {
    return label;
  }

  public long getCount() {
    return count;
  }

  @Override
  public int compareTo(LabelCountEntry o) {
    return Long.compare(this.count, o.count);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("LabelCountEntry(");
    sb.append(label);
    sb.append(",");
    sb.append(count);
    sb.append(")");
    return sb.toString();
  }

  @JsonCreator
  public static LabelCountEntry build(
      @JsonProperty("label") String label, @JsonProperty("count") long count) {
    if (label == null || label.trim().length() == 0) {
      throw new IllegalArgumentException("label must not be blank");
    }
    return new LabelCountEntry(label.trim(), count);
  }

  @JsonCreator
  public static LabelCountEntry build(Map<String, String> entryMap) {
    if (isValidLabelCount(entryMap)) {
      return new LabelCountEntry(entryMap.get("label"), Long.parseLong(entryMap.get("count")));
    }
    throw new RuntimeException(
        "Provided object is not suitable to be converted to LabelCountEntry");
  }

  public static Boolean isValidLabelCount(Map<String, String> content) {
    return content.keySet().containsAll(Arrays.asList("label", "count"));
  }
}
