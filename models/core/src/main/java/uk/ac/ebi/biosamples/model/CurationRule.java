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

public class CurationRule implements Comparable<CurationRule> {
  private String attributePre;
  private String attributePost;

  private CurationRule(String attributePre, String attributePost) {
    this.attributePre = attributePre;
    this.attributePost = attributePost;
  }

  public String getAttributePre() {
    return attributePre;
  }

  public String getAttributePost() {
    return attributePost;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof CurationRule)) {
      return false;
    }
    CurationRule other = (CurationRule) o;
    return Objects.equals(
        this.attributePre, other.attributePre); // Attribute pre should be unique to pick a rule
  }

  @Override
  public int hashCode() {
    return Objects.hash(attributePre);
  }

  @Override
  public int compareTo(CurationRule other) {
    if (other == null) {
      return 1;
    } else if (this.attributePre.equals(other.attributePre)) {
      return 0;
    } else {
      return this.attributePre.compareTo(other.attributePre);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("CurationRule(");
    sb.append(this.attributePre);
    sb.append(",");
    sb.append(this.attributePost);
    sb.append(")");
    return sb.toString();
  }

  @JsonCreator
  public static CurationRule build(
      @JsonProperty("attributePre") String attributePre,
      @JsonProperty("attributePost") String attributePost) {
    return new CurationRule(attributePre, attributePost);
  }
}
