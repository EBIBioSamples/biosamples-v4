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

import com.fasterxml.jackson.annotation.*;
import java.util.Optional;
import uk.ac.ebi.biosamples.model.facet.content.FacetContent;
import uk.ac.ebi.biosamples.model.filter.FilterType;

// TODO constant this
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AttributeFacet.class, name = "attribute"),
  @JsonSubTypes.Type(value = RelationFacet.class, name = "relation"),
  @JsonSubTypes.Type(value = InverseRelationFacet.class, name = "inverse relation"),
  @JsonSubTypes.Type(value = ExternalReferenceDataFacet.class, name = "external reference data")
})
@JsonPropertyOrder(value = {"type", "label", "count", "content"})
public interface Facet extends Comparable<Facet> {

  @JsonProperty("type")
  public FacetType getType();

  public String getLabel();

  public Long getCount();

  public FacetContent getContent();

  //    @JsonIgnore
  //    public default Optional<FilterType> getAssociatedFilterType() {
  //        return FacetFilterFieldType.getFilterForFacet(this.getType());
  //    }

  @JsonIgnore
  public Optional<FilterType> getAssociatedFilterType();

  @JsonIgnore
  public String getContentSerializableFilter(String label);

  /*
   * Builder interface to build Facets
   */
  public interface Builder {
    Facet build();

    Builder withContent(FacetContent content);
  }

  @Override
  default int compareTo(final Facet otherFacet) {
    return FacetHelper.compareFacets(getLabel(), otherFacet.getLabel());
    //    return Long.compare(this.getCount(), otherFacet.getCount());
  }
}
