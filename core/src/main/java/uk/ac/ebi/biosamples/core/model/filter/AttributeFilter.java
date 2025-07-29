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

public class AttributeFilter implements Filter {

  private final String label;
  private final String value;

  private AttributeFilter(final String label, final String value) {
    this.label = label;
    this.value = value;
  }

  @Override
  public FilterType getType() {
    return FilterType.ATTRIBUTE_FILTER;
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
    return FacetType.ATTRIBUTE_FACET;
  }

  @Override
  public String getSerialization() {
    final StringBuilder serialization =
        new StringBuilder(getType().getSerialization()).append(":").append(label);
    getContent().ifPresent(value -> serialization.append(":").append(value));
    return serialization.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof AttributeFilter)) {
      return false;
    }
    final AttributeFilter other = (AttributeFilter) obj;
    return Objects.equals(other.label, label) && Objects.equals(other.value, value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, value);
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
    public AttributeFilter build() {
      return new AttributeFilter(label, value);
    }

    @Override
    public Builder parseContent(final String filterValue) {
      return withValue(filterValue);
    }
  }
}
