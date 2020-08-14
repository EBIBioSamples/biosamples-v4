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
package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class Relationship implements Comparable<Relationship> {

  private final String type;
  private final String target;
  private final String source;

  private Relationship(String type, String target, String source) {

    this.type = type;
    this.target = target;
    this.source = source;
  }

  public String getType() {
    return type;
  }

  public String getTarget() {
    return target;
  }

  public String getSource() {
    return source;
  }

  @Override
  public boolean equals(Object o) {

    if (o == this) return true;
    if (!(o instanceof Relationship)) {
      return false;
    }
    Relationship other = (Relationship) o;
    return Objects.equals(this.type, other.type)
        && Objects.equals(this.target, other.target)
        && Objects.equals(this.source, other.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, target, source);
  }

  @Override
  public int compareTo(Relationship other) {
    if (other == null) {
      return 1;
    }
    if (!Objects.equals(this.type, other.type)) {
      return this.type.compareTo(other.type);
    }

    if (!Objects.equals(this.target, other.target)) {
      return this.target.compareTo(other.target);
    }

    if (!Objects.equals(this.source, other.source)) {
      if (this.source == null && other.source != null) {
        return 1;
      } else if (this.source != null && other.source == null) {
        return -1;
      } else {
        return this.source.compareTo(other.source);
      }
    }
    return 0;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
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
      @JsonProperty("source") String source,
      @JsonProperty("type") String type,
      @JsonProperty("target") String target) {
    if (type == null || type.trim().length() == 0)
      throw new IllegalArgumentException("type cannot be empty");
    if (target == null || target.trim().length() == 0)
      throw new IllegalArgumentException("target cannot be empty");
    return new Relationship(type, target, source);
  }

  public static class Builder {
    private String source = null;
    private String target = null;
    private String type = null;

    public Builder() {}

    public Builder withSource(String source) {
      this.source = source;
      return this;
    }

    public Builder withTarget(String target) {
      this.target = target;
      return this;
    }

    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    public Relationship build() {
      return Relationship.build(source, type, target);
    }
  }
}
