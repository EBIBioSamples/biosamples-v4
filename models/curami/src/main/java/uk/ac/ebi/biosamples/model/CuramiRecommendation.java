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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.SortedSet;
import java.util.TreeSet;

public class CuramiRecommendation {
  @JsonProperty("SampleQuality")
  private int quality;

  @JsonProperty("knownAttributes")
  private SortedSet<String> knownAttributes;

  @JsonProperty("AttributeRecommendations")
  private SortedSet<AttributeRecommendation> attributeRecommendations;

  @JsonProperty("unknownAttributes")
  private SortedSet<String> unknownAttributes;

  private CuramiRecommendation(
      int quality,
      SortedSet<String> knownAttributes,
      SortedSet<AttributeRecommendation> attributeRecommendations,
      SortedSet<String> unknownAttributes) {
    this.quality = quality;
    this.knownAttributes = knownAttributes;
    this.attributeRecommendations = attributeRecommendations;
    this.unknownAttributes = unknownAttributes;
  }

  public int getQuality() {
    return quality;
  }

  public SortedSet<String> getKnownAttributes() {
    return knownAttributes;
  }

  public SortedSet<AttributeRecommendation> getAttributeRecommendations() {
    return attributeRecommendations;
  }

  public SortedSet<String> getUnknownAttributes() {
    return unknownAttributes;
  }

  public static class Builder {
    private int quality;
    private SortedSet<String> goodAttributes;
    private SortedSet<AttributeRecommendation> badAttributes;
    private SortedSet<String> missingAttributes;

    public Builder() {
      this.goodAttributes = new TreeSet<>();
      this.badAttributes = new TreeSet<>();
      this.missingAttributes = new TreeSet<>();
    }

    public Builder withQuality(int quality) {
      this.quality = Math.min(Math.max(quality, 0), 100);
      return this;
    }

    public Builder withGoodAttributes(SortedSet<String> goodAttributes) {
      this.goodAttributes = goodAttributes;
      return this;
    }

    public Builder withBadAttributes(SortedSet<AttributeRecommendation> badAttributes) {
      this.badAttributes = badAttributes;
      return this;
    }

    public Builder withMissingAttributes(SortedSet<String> missingAttributes) {
      this.missingAttributes = missingAttributes;
      return this;
    }

    public CuramiRecommendation build() {
      return new CuramiRecommendation(quality, goodAttributes, badAttributes, missingAttributes);
    }
  }
}
