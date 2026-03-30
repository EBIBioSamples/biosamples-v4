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
package uk.ac.ebi.biosamples.core.model.filter;

import java.util.Objects;
import java.util.Optional;
import uk.ac.ebi.biosamples.core.model.facet.FacetType;

public class StructuredDataFilter implements Filter {

  private final String dataType;

  private StructuredDataFilter(final String dataType) {
    this.dataType = dataType;
  }

  @Override
  public FilterType getType() {
    return FilterType.STRUCTURED_DATA_FILTER;
  }

  @Override
  public String getLabel() {
    return "structured data";
  }

  @Override
  public Optional<String> getContent() {
    return Optional.ofNullable(dataType);
  }

  @Override
  public FacetType getAssociatedFacetType() {
    return FacetType.NO_TYPE;
  }

  @Override
  public String getSerialization() {
    final StringBuilder serialization =
        new StringBuilder(getType().getSerialization()).append(":").append(getLabel());
    getContent().ifPresent(value -> serialization.append(":").append(value));
    return serialization.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof StructuredDataFilter)) {
      return false;
    }
    final StructuredDataFilter other = (StructuredDataFilter) obj;
    return Objects.equals(other.dataType, dataType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dataType);
  }

  public static class Builder implements Filter.Builder {
    private String dataType;

    public Builder() {}

    public Builder(final String label) {
      // Label parameter required by FilterType.getBuilderForLabel() but not used
      // since label is always "structured data"
    }

    public Builder withDataType(final String dataType) {
      this.dataType = dataType;
      return this;
    }

    @Override
    public StructuredDataFilter build() {
      return new StructuredDataFilter(dataType);
    }

    @Override
    public Builder parseContent(final String filterValue) {
      return withDataType(filterValue);
    }
  }
}
