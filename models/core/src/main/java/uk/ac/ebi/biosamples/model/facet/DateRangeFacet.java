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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.hateoas.core.Relation;
import uk.ac.ebi.biosamples.model.facet.content.FacetContent;
import uk.ac.ebi.biosamples.model.facet.content.LabelCountEntry;
import uk.ac.ebi.biosamples.model.facet.content.LabelCountListContent;
import uk.ac.ebi.biosamples.model.filter.FilterType;

@Relation(collectionRelation = "facets")
@JsonDeserialize(builder = DateRangeFacet.Builder.class)
public class DateRangeFacet implements Facet {

  private String facetLabel;
  private Long facetCount;
  private LabelCountListContent content;

  private DateRangeFacet(String facetLabel, Long facetCount, LabelCountListContent content) {
    this.facetLabel = facetLabel;
    this.facetCount = facetCount;
    this.content = content;
  }

  @Override
  public FacetType getType() {
    return FacetType.DATE_RANGE_FACET;
  }

  @Override
  public Optional<FilterType> getAssociatedFilterType() {
    return Optional.of(FilterType.DATE_FILTER);
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
    String[] range = label.split(" to ");
    return "from=" + range[0] + "until=" + range[1];
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("DateRangeFacet(");
    sb.append(facetLabel);
    sb.append(",");
    sb.append(facetCount);
    sb.append(",");
    sb.append(content);
    sb.append(")");
    return sb.toString();
  }

  public static class Builder implements Facet.Builder {

    private String field;
    private Long count;
    private LabelCountListContent content = null;

    @JsonCreator
    public Builder(@JsonProperty("label") String field, @JsonProperty("count") Long count) {
      this.field = field;
      this.count = count;
    }

    @JsonProperty
    @Override
    public Builder withContent(FacetContent content) {

      if (!(content instanceof LabelCountListContent)) {
        throw new RuntimeException("Content not compatible with an attribute facet");
      }

      LabelCountListContent tempContent = (LabelCountListContent) content;
      List<LabelCountEntry> contentList = new ArrayList<>();
      for (int i = 0; i < tempContent.size(); i++) {
        LabelCountEntry entry = tempContent.get(i);
        contentList.add(
            LabelCountEntry.build(parseLabelToDateRange(entry.getLabel()), entry.getCount()));
      }

      this.content = new LabelCountListContent(contentList);
      return this;
    }

    private String parseLabelToDateRange(String label) {
      String dateLabel = label.substring(0, 10);
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      LocalDate start = LocalDate.parse(dateLabel, formatter);
      LocalDate end = start.plusYears(1);
      // LocalDateTime dateTime = LocalDateTime.parse(dateLabel, formatter);

      return start + " to " + end;
    }

    @Override
    public Facet build() {
      return new DateRangeFacet(this.field, this.count, this.content);
    }
  }
}
