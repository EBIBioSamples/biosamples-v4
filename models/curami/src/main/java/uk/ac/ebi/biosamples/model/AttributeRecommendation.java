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

import java.util.Objects;

public class AttributeRecommendation implements Comparable<AttributeRecommendation> {
  private final String attribute;
  private final String recommendation;

  private AttributeRecommendation(String attribute, String recommendations) {
    this.attribute = attribute;
    this.recommendation = recommendations;
  }

  public String getAttribute() {
    return attribute;
  }

  public String getRecommendation() {
    return recommendation;
  }

  @Override
  public String toString() {
    return attribute + "->" + recommendation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof AttributeRecommendation) {
      AttributeRecommendation other = (AttributeRecommendation) o;
      return Objects.equals(this.getAttribute(), other.getAttribute())
          && Objects.equals(this.getRecommendation(), other.getRecommendation());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(attribute);
  }

  @Override
  public int compareTo(AttributeRecommendation other) {
    return this.attribute.compareTo(other.attribute);
  }

  public static class Builder {
    private String attribute;
    private String recommendation;

    public Builder withAttribute(String attribute) {
      this.attribute = attribute;
      return this;
    }

    public Builder withRecommendation(String recommendations) {
      this.recommendation = recommendations;
      return this;
    }

    public AttributeRecommendation build() {
      return new AttributeRecommendation(attribute, recommendation);
    }
  }
}
