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

public class AccessionFilter implements Filter {

  private String accessionPattern;

  private AccessionFilter(String accessionPattern) {
    this.accessionPattern = accessionPattern;
  }

  @Override
  public FilterType getType() {
    return FilterType.ACCESSION_FILTER;
  }

  @Override
  public String getLabel() {
    return "id";
  }

  @Override
  public Optional<String> getContent() {
    return Optional.of(this.accessionPattern);
  }

  @Override
  public FacetType getAssociatedFacetType() {
    return FacetType.NO_TYPE;
  }

  @Override
  public String getSerialization() {
    return this.getType().getSerialization() + ":" + this.accessionPattern;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof AccessionFilter)) {
      return false;
    }
    AccessionFilter other = (AccessionFilter) obj;
    return Objects.equals(other.getContent().orElse(null), this.getContent().orElse(null));
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getContent().orElse(null));
  }

  public static class Builder implements Filter.Builder {

    private String pattern;

    public Builder(String pattern) {
      this.pattern = pattern;
    }

    @Override
    public Filter build() {
      return new AccessionFilter(this.pattern);
    }

    @Override
    public Filter.Builder parseContent(String filterSerialized) {
      return new Builder(filterSerialized);
    }
  }
}
