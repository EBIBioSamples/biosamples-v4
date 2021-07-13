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
package uk.ac.ebi.biosamples.model.facet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Optional;
import org.springframework.hateoas.core.Relation;
import uk.ac.ebi.biosamples.model.facet.content.FacetContent;
import uk.ac.ebi.biosamples.model.facet.content.LabelCountListContent;
import uk.ac.ebi.biosamples.model.filter.FilterType;

@Relation(collectionRelation = "facets")
@JsonDeserialize(builder = InverseRelationFacet.Builder.class)
public class InverseRelationFacet implements Facet {

  private String facetLabel;
  private Long facetCount;
  private LabelCountListContent content;

  private InverseRelationFacet(String facetLabel, Long facetCount, LabelCountListContent content) {
    this.facetLabel = facetLabel;
    this.facetCount = facetCount;
    this.content = content;
  }

  @Override
  public FacetType getType() {
    return FacetType.INVERSE_RELATION_FACET;
  }

  @Override
  public Optional<FilterType> getAssociatedFilterType() {
    return Optional.of(FilterType.INVERSE_RELATION_FILTER);
  }

  @Override
  public String getLabel() {
    return this.facetLabel;
  }

  @Override
  public Long getCount() {
    return this.facetCount;
  }

  @Override
  public LabelCountListContent getContent() {
    return this.content;
  }

  @Override
  public String getContentSerializableFilter(String label) {
    return label;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("InverseRelationFacet(");
    sb.append(facetLabel);
    sb.append(",");
    sb.append(facetCount);
    sb.append(",");
    sb.append(content);
    sb.append(")");
    return sb.toString();
  }

  public static class Builder implements Facet.Builder {

    private String label;
    private Long count;
    private LabelCountListContent content = null;

    @JsonCreator
    public Builder(@JsonProperty("label") String label, @JsonProperty("count") Long count) {
      this.label = label;
      this.count = count;
    }

    @JsonProperty
    @Override
    public Builder withContent(FacetContent content) {
      if (!(content instanceof LabelCountListContent)) {
        throw new RuntimeException("Content not compatible with an inverse relation facet");
      }

      this.content = (LabelCountListContent) content;
      return this;
    }

    @Override
    public Facet build() {
      return new InverseRelationFacet(this.label, this.count, this.content);
    }
  }
}
