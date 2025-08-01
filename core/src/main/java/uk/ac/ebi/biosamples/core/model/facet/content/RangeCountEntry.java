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
package uk.ac.ebi.biosamples.core.model.facet.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Map;

public class RangeCountEntry implements Comparable<RangeCountEntry> {
  private final String startLabel;
  private final String endLabel;
  private final long count;

  private RangeCountEntry(final String startLabel, final String endLabel, final long count) {
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
  public int compareTo(final RangeCountEntry o) {
    return Long.compare(count, o.count);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();

    sb.append("LabelCountEntry(");
    sb.append(getLabel());
    sb.append(",");
    sb.append(count);
    sb.append(")");

    return sb.toString();
  }

  @JsonCreator
  public static RangeCountEntry build(
      @JsonProperty("label") final String startLabel,
      @JsonProperty("label") final String endLabel,
      @JsonProperty("count") final long count) {
    if (startLabel == null
        || startLabel.trim().isEmpty()
        || endLabel == null
        || endLabel.trim().isEmpty()) {
      throw new IllegalArgumentException("start/end label must not be blank");
    }

    return new RangeCountEntry(startLabel.trim(), endLabel.trim(), count);
  }

  @JsonCreator
  public static RangeCountEntry build(final Map<String, String> entryMap) {
    if (isValidLabelCount(entryMap)) {
      return new RangeCountEntry(
          entryMap.get("startLabel"),
          entryMap.get("endLabel"),
          Long.parseLong(entryMap.get("count")));
    }
    throw new RuntimeException(
        "Provided object is not suitable to be converted to RangeCountEntry");
  }

  private static boolean isValidLabelCount(final Map<String, String> content) {
    return content.keySet().containsAll(Arrays.asList("startLabel", "endLabel", "count"));
  }
}
