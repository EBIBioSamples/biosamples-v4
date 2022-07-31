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
package uk.ac.ebi.biosamples.model.facet.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Map;

public class RangeCountEntry implements Comparable<RangeCountEntry> {
  private final String startLabel;
  private final String endLabel;
  private final long count;

  private RangeCountEntry(String startLabel, String endLabel, long count) {
    this.startLabel = startLabel;
    this.endLabel = endLabel;
    this.count = count;
  }

  public String getStartLabel() {
    return startLabel;
  }

  public String getEndLabel() {
    return endLabel;
  }

  public long getCount() {
    return count;
  }

  public String getLabel() {
    return startLabel + " TO " + endLabel;
  }

  @Override
  public int compareTo(RangeCountEntry o) {
    return Long.compare(this.count, o.count);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("LabelCountEntry(");
    sb.append(getLabel());
    sb.append(",");
    sb.append(count);
    sb.append(")");
    return sb.toString();
  }

  @JsonCreator
  public static RangeCountEntry build(
      @JsonProperty("label") String startLabel,
      @JsonProperty("label") String endLabel,
      @JsonProperty("count") long count) {
    if (startLabel == null
        || startLabel.trim().length() == 0
        || endLabel == null
        || endLabel.trim().length() == 0) {
      throw new IllegalArgumentException("start/end label must not be blank");
    }
    return new RangeCountEntry(startLabel.trim(), endLabel.trim(), count);
  }

  @JsonCreator
  public static RangeCountEntry build(Map<String, String> entryMap) {
    if (isValidLabelCount(entryMap)) {
      return new RangeCountEntry(
          entryMap.get("startLabel"),
          entryMap.get("endLabel"),
          Long.parseLong(entryMap.get("count")));
    }
    throw new RuntimeException(
        "Provided object is not suitable to be converted to RangeCountEntry");
  }

  public static boolean isValidLabelCount(Map<String, String> content) {
    return content.keySet().containsAll(Arrays.asList("startLabel", "endLabel", "count"));
  }
}
