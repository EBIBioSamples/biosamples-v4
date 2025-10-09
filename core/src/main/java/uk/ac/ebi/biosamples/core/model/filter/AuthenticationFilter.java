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

public class AuthenticationFilter implements Filter {
  private final String authInfo;

  private AuthenticationFilter(final String authInfo) {
    this.authInfo = authInfo;
  }

  @Override
  public FilterType getType() {
    if (authInfo.toLowerCase().startsWith("webin")) {
      return FilterType.WEBINID_FILTER;
    }

    return FilterType.DOMAIN_FILTER;
  }

  @Override
  public String getLabel() {
    if (authInfo.toLowerCase().startsWith("webin")) {
      return "webinId";
    }

    return "domain";
  }

  @Override
  public Optional<String> getContent() {
    return Optional.of(authInfo);
  }

  @Override
  public String getSerialization() {
    return getType().getSerialization() + ":" + authInfo;
  }

  @Override
  public FacetType getAssociatedFacetType() {
    return FacetType.NO_TYPE;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof AuthenticationFilter)) {
      return false;
    }
    final AuthenticationFilter other = (AuthenticationFilter) obj;
    return Objects.equals(other.getContent().orElse(null), getContent().orElse(null));
  }

  @Override
  public int hashCode() {
    return Objects.hash(getContent().orElse(null));
  }

  public static class Builder implements Filter.Builder {

    private final String authInfo;

    public Builder(final String authInfo) {
      this.authInfo = authInfo;
    }

    @Override
    public Filter build() {
      return new AuthenticationFilter(authInfo);
    }

    @Override
    public Filter.Builder parseContent(final String filterSerialized) {
      return new Builder(filterSerialized);
    }
  }
}
