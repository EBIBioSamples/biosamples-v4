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
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import lombok.Getter;

@Getter
public class Relationship implements Comparable<Relationship> {
  private final String type;
  private final String target;
  private final String source;

  private Relationship(final String type, final String target, final String source) {

    this.type = type;
    this.target = target;
    this.source = source;
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }
    if (!(o instanceof Relationship)) {
      return false;
    }
    final Relationship other = (Relationship) o;
    return Objects.equals(type, other.type)
        && Objects.equals(target, other.target)
        && Objects.equals(source, other.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, target, source);
  }

  @Override
  public int compareTo(final Relationship other) {
    if (other == null) {
      return 1;
    }
    if (!Objects.equals(type, other.type)) {
      return type.compareTo(other.type);
    }

    if (!Objects.equals(target, other.target)) {
      return target.compareTo(other.target);
    }

    if (!Objects.equals(source, other.source)) {
      if (source == null) {
        return 1;
      } else if (other.source == null) {
        return -1;
      } else {
        return source.compareTo(other.source);
      }
    }
    return 0;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Relationships(");
    sb.append(source);
    sb.append(",");
    sb.append(type);
    sb.append(",");
    sb.append(target);
    sb.append(")");
    return sb.toString();
  }

  @JsonCreator
  public static Relationship build(
      @JsonProperty("source") final String source,
      @JsonProperty("type") final String type,
      @JsonProperty("target") final String target) {
    if (type == null || type.trim().isEmpty()) {
      throw new IllegalArgumentException("type cannot be empty");
    }
    if (target == null || target.trim().isEmpty()) {
      throw new IllegalArgumentException("target cannot be empty");
    }
    return new Relationship(type, target, source);
  }

  public static class Builder {
    private String source = null;
    private String target = null;
    private String type = null;

    public Builder() {}

    public Builder withSource(final String source) {
      this.source = source;
      return this;
    }

    public Builder withTarget(final String target) {
      this.target = target;
      return this;
    }

    public Builder withType(final String type) {
      this.type = type;
      return this;
    }

    public Relationship build() {
      return Relationship.build(source, type, target);
    }
  }
}
