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
package uk.ac.ebi.biosamples.core.model.facet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Optional;
import uk.ac.ebi.biosamples.core.model.facet.content.FacetContent;
import uk.ac.ebi.biosamples.core.model.facet.content.LabelCountListContent;
import uk.ac.ebi.biosamples.core.model.filter.FilterType;

@JsonDeserialize(builder = AttributeFacet.Builder.class)
public class ExternalReferenceDataFacet implements Facet {
  private final String facetLabel;
  private final Long facetCount;
  private final LabelCountListContent content;

  private ExternalReferenceDataFacet(
      final String facetLabel, final Long facetCount, final LabelCountListContent content) {
    this.facetLabel = facetLabel;
    this.facetCount = facetCount;
    this.content = content;
  }

  @Override
  public FacetType getType() {
    return FacetType.EXTERNAL_REFERENCE_DATA_FACET;
  }

  @Override
  public Optional<FilterType> getAssociatedFilterType() {
    return Optional.of(FilterType.EXTERNAL_REFERENCE_DATA_FILTER);
  }

  @Override
  public String getLabel() {
    return facetLabel;
  }

  @Override
  public Long getCount() {
    return facetCount;
  }

  @Override
  public LabelCountListContent getContent() {
    return content;
  }

  @Override
  public String getContentSerializableFilter(final String label) {
    return label;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ExternalReferenceDataFacet(");
    sb.append(facetLabel);
    sb.append(",");
    sb.append(facetCount);
    sb.append(",");
    sb.append(content);
    sb.append(")");
    return sb.toString();
  }

  public static class Builder implements Facet.Builder {

    private final String field;
    private final Long count;
    private LabelCountListContent content = null;

    @JsonCreator
    public Builder(
        @JsonProperty("label") final String field, @JsonProperty("count") final Long count) {
      this.field = field;
      this.count = count;
    }

    @JsonProperty
    @Override
    public Builder withContent(final FacetContent content) {

      if (!(content instanceof LabelCountListContent)) {
        throw new RuntimeException("Content not compatible with an attribute facet");
      }

      this.content = (LabelCountListContent) content;
      return this;
    }

    @Override
    public Facet build() {
      return new ExternalReferenceDataFacet(field, count, content);
    }
  }
}
