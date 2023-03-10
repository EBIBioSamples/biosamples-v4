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
package uk.ac.ebi.biosamples.model.filter;

import java.util.Objects;
import java.util.Optional;
import uk.ac.ebi.biosamples.model.facet.FacetType;

public class InverseRelationFilter implements Filter {

  private final String label;
  private final String value;

  private InverseRelationFilter(final String label, final String value) {
    this.label = label;
    this.value = value;
  }

  @Override
  public FilterType getType() {
    return FilterType.INVERSE_RELATION_FILTER;
  }

  @Override
  public String getLabel() {
    return label;
  }

  @Override
  public Optional<String> getContent() {
    return Optional.ofNullable(value);
  }

  @Override
  public FacetType getAssociatedFacetType() {
    return FacetType.INVERSE_RELATION_FACET;
  }

  @Override
  public String getSerialization() {
    final StringBuilder serializationBuilder =
        new StringBuilder(getType().getSerialization()).append(":").append(getLabel());
    getContent().ifPresent(content -> serializationBuilder.append(":").append(content));
    return serializationBuilder.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof InverseRelationFilter)) {
      return false;
    }
    final InverseRelationFilter other = (InverseRelationFilter) obj;
    return Objects.equals(other.getLabel(), getLabel())
        && Objects.equals(other.getContent().orElse(null), getContent().orElse(null));
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLabel(), getContent().orElse(null));
  }

  public static class Builder implements Filter.Builder {
    private String value;
    private final String label;

    public Builder(final String label) {
      this.label = label;
    }

    public Builder withValue(final String value) {
      this.value = value;
      return this;
    }

    @Override
    public InverseRelationFilter build() {
      return new InverseRelationFilter(label, value);
    }

    @Override
    public Builder parseContent(final String filterValue) {
      return withValue(filterValue);
    }
  }
}
