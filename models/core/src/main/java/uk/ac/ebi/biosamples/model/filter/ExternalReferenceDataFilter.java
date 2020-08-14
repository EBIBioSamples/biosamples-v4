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
package uk.ac.ebi.biosamples.model.filter;

import java.util.Objects;
import java.util.Optional;
import uk.ac.ebi.biosamples.model.facet.FacetType;

public class ExternalReferenceDataFilter implements Filter {
  private String label;
  private String value;

  private ExternalReferenceDataFilter(String label, String value) {
    this.label = label;
    this.value = value;
  }

  @Override
  public FilterType getType() {
    return FilterType.EXTERNAL_REFERENCE_DATA_FILTER;
  }

  @Override
  public String getLabel() {
    return this.label;
  }

  @Override
  public Optional<String> getContent() {
    return Optional.ofNullable(this.value);
  }

  @Override
  public FacetType getAssociatedFacetType() {
    return FacetType.EXTERNAL_REFERENCE_DATA_FACET;
  }

  @Override
  public String getSerialization() {
    StringBuilder serialization =
        new StringBuilder(this.getType().getSerialization()).append(":").append(this.label);
    this.getContent().ifPresent(value -> serialization.append(":").append(value));
    return serialization.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ExternalReferenceDataFilter)) {
      return false;
    }
    ExternalReferenceDataFilter other = (ExternalReferenceDataFilter) obj;
    return Objects.equals(other.label, this.label)
        && Objects.equals(other.getContent().orElse(null), this.getContent().orElse(null));
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.label, this.getContent().orElse(null));
  }

  public static class Builder implements Filter.Builder {
    private String value;
    private String label;

    public Builder(String label) {
      this.label = label;
    }

    public Builder withValue(String value) {
      this.value = value;
      return this;
    }

    @Override
    public ExternalReferenceDataFilter build() {
      return new ExternalReferenceDataFilter(this.label, this.value);
    }

    @Override
    public Builder parseContent(String filterValue) {
      return this.withValue(filterValue);
    }
  }
}
