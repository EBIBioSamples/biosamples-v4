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
public class CurationRule implements Comparable<CurationRule> {
  private final String attributePre;
  private final String attributePost;

  private CurationRule(final String attributePre, final String attributePost) {
    this.attributePre = attributePre;
    this.attributePost = attributePost;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof CurationRule)) {
      return false;
    }
    final CurationRule other = (CurationRule) o;
    return Objects.equals(
        attributePre, other.attributePre); // Attribute pre should be unique to pick a rule
  }

  @Override
  public int hashCode() {
    return Objects.hash(attributePre);
  }

  @Override
  public int compareTo(final CurationRule other) {
    if (other == null) {
      return 1;
    } else if (attributePre.equals(other.attributePre)) {
      return 0;
    } else {
      return attributePre.compareTo(other.attributePre);
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("CurationRule(");
    sb.append(attributePre);
    sb.append(",");
    sb.append(attributePost);
    sb.append(")");
    return sb.toString();
  }

  @JsonCreator
  public static CurationRule build(
      @JsonProperty("attributePre") final String attributePre,
      @JsonProperty("attributePost") final String attributePost) {
    return new CurationRule(attributePre, attributePost);
  }
}
