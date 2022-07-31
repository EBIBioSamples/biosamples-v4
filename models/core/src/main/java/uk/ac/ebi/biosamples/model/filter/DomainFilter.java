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

public class DomainFilter implements Filter {

  private String domain;

  private DomainFilter(String domain) {
    this.domain = domain;
  }

  @Override
  public FilterType getType() {
    return FilterType.DOMAIN_FILTER;
  }

  @Override
  public String getLabel() {
    return "domain";
  }

  @Override
  public Optional<String> getContent() {
    return Optional.of(this.domain);
  }

  @Override
  public String getSerialization() {
    return this.getType().getSerialization() + ":" + this.domain;
  }

  @Override
  public FacetType getAssociatedFacetType() {
    return FacetType.NO_TYPE;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof DomainFilter)) {
      return false;
    }
    DomainFilter other = (DomainFilter) obj;
    return Objects.equals(other.getContent().orElse(null), this.getContent().orElse(null));
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getContent().orElse(null));
  }

  public static class Builder implements Filter.Builder {

    private String domain;

    public Builder(String domain) {
      this.domain = domain;
    }

    @Override
    public Filter build() {
      return new DomainFilter(this.domain);
    }

    @Override
    public Filter.Builder parseContent(String filterSerialized) {
      return new Builder(filterSerialized);
    }
  }
}
