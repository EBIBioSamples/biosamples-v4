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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import java.util.*;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Attribute implements Comparable<Attribute> {
  // TODO: This needs to be public static otherwise spring-data-mongo goes crazy
  public static Logger log = LoggerFactory.getLogger(Attribute.class);

  @JsonProperty("type")
  private String type;

  @JsonProperty("value")
  private String value;

  @JsonProperty("iri")
  private SortedSet<String> iri;

  @JsonProperty("unit")
  private String unit;

  @JsonProperty("tag")
  private String tag;

  private Attribute() {}

  @Override
  public int compareTo(final Attribute other) {
    if (other == null) {
      return 1;
    }

    int comparison = nullSafeStringComparison(type, other.type);

    if (comparison != 0) {
      return comparison;
    }

    comparison = nullSafeStringComparison(value, other.value);

    if (comparison != 0) {
      return comparison;
    }

    comparison = nullSafeStringComparison(tag, other.tag);

    if (comparison != 0) {
      return comparison;
    }

    if (iri == null && other.iri != null) {
      return -1;
    }

    if (iri != null && other.iri == null) {
      return 1;
    }

    assert iri != null;

    if (!iri.equals(other.iri)) {
      if (iri.size() < other.iri.size()) {
        return -1;
      } else if (iri.size() > other.iri.size()) {
        return 1;
      } else {
        final Iterator<String> thisIt = iri.iterator();
        final Iterator<String> otherIt = other.iri.iterator();

        while (thisIt.hasNext() && otherIt.hasNext()) {
          final int val = thisIt.next().compareTo(otherIt.next());

          if (val != 0) {
            return val;
          }
        }
      }
    }

    return nullSafeStringComparison(unit, other.unit);
  }

  private int nullSafeStringComparison(final String one, final String two) {
    if (one == null && two != null) {
      return -1;
    }

    if (one != null && two == null) {
      return 1;
    }

    if (one != null && !one.equals(two)) {
      return one.compareTo(two);
    }

    return 0;
  }

  public static Attribute build(final String type, final String value) {
    return build(type, value, null, Lists.newArrayList(), null);
  }

  public static Attribute build(
      final String type, final String value, String iri, final String unit) {
    if (iri == null) {
      iri = "";
    }

    return build(type, value, null, Lists.newArrayList(iri), unit);
  }

  public static Attribute build(
      final String type, final String value, final String tag, String iri, final String unit) {
    if (iri == null) {
      iri = "";
    }

    return build(type, value, tag, Lists.newArrayList(iri), unit);
  }

  @JsonCreator
  public static Attribute build(
      @JsonProperty("type") String type,
      @JsonProperty("value") String value,
      @JsonProperty("tag") String tag,
      @JsonProperty("iri") Collection<String> iri,
      @JsonProperty("unit") String unit) {
    // check for nulls
    if (type == null) {
      throw new IllegalArgumentException("type must not be null");
    }

    if (value == null) {
      value = "";
    }

    if (iri == null) {
      iri = Lists.newArrayList();
    }
    // cleanup inputs
    type = type.trim();
    value = value.trim();

    if (tag != null) {
      tag = tag.trim();
    }

    if (unit != null) {
      unit = unit.trim();
    }
    // create output
    final Attribute attr = new Attribute();

    attr.type = type;
    attr.value = value;
    attr.tag = tag;
    attr.iri = new TreeSet<>();

    for (final String iriOne : iri) {
      if (iriOne != null) {
        attr.iri.add(iriOne);
      }
    }

    attr.unit = unit;
    return attr;
  }

  public static class Builder {
    private final String type;
    private final String value;
    private final List<String> iris = new ArrayList<>();
    private String unit;
    private String tag;

    public Builder(final String type, final String value, final String tag) {
      this.type = type;
      this.value = value;
      this.tag = tag;
    }

    public Builder(final String type, final String value) {
      this.type = type;
      this.value = value;
    }

    public Builder withIri(final String iri) {
      iris.add(iri);
      return this;
    }

    public Builder withUnit(final String unit) {
      this.unit = unit;
      return this;
    }

    public Builder withTag(final String tag) {
      this.tag = tag;
      return this;
    }

    public Attribute build() {
      return Attribute.build(type, value, tag, iris, unit);
    }
  }
}
