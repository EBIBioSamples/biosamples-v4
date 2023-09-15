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
package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import java.util.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalReference implements Comparable<ExternalReference> {
  private final String url;
  private final String hash;
  private final SortedSet<String> duo;

  private ExternalReference(final String url, final String hash, final SortedSet<String> duo) {
    this.url = url;
    this.hash = hash;
    this.duo = duo;
  }

  public String getUrl() {
    return url;
  }

  @JsonIgnore
  public String getHash() {
    return hash;
  }

  public SortedSet<String> getDuo() {
    return duo;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ExternalReference)) {
      return false;
    }
    final ExternalReference other = (ExternalReference) o;
    return Objects.equals(url, other.url) && Objects.equals(duo, other.duo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hash);
  }

  @Override
  public int compareTo(final ExternalReference other) {
    if (other == null) {
      return 1;
    }

    if (!url.equals(other.url)) {
      return url.compareTo(other.url);
    }

    if (duo == other.duo) {
      return 0;
    } else if (other.duo == null) {
      return 1;
    } else if (duo == null) {
      return -1;
    }

    if (!duo.equals(other.duo)) {
      if (duo.size() < other.duo.size()) {
        return -1;
      } else if (duo.size() > other.duo.size()) {
        return 1;
      } else {
        final Iterator<String> thisIt = duo.iterator();
        final Iterator<String> otherIt = other.duo.iterator();
        while (thisIt.hasNext() && otherIt.hasNext()) {
          final int val = thisIt.next().compareTo(otherIt.next());
          if (val != 0) {
            return val;
          }
        }
      }
    }
    return 0;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ExternalReference(");
    sb.append(url);
    sb.append(",");
    sb.append(duo);
    sb.append(")");
    return sb.toString();
  }

  public static ExternalReference build(String url, SortedSet<String> duo) {
    final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(url);
    final UriComponents uriComponents = uriComponentsBuilder.build().normalize();

    url = uriComponents.toUriString();

    final Hasher hasher =
        Hashing.sha256()
            .newHasher()
            .putUnencodedChars(
                Objects.nonNull(uriComponents.getScheme()) ? uriComponents.getScheme() : "")
            .putUnencodedChars(
                Objects.nonNull(uriComponents.getSchemeSpecificPart())
                    ? uriComponents.getSchemeSpecificPart()
                    : "")
            .putUnencodedChars(
                Objects.nonNull(uriComponents.getUserInfo()) ? uriComponents.getUserInfo() : "")
            .putUnencodedChars(
                Objects.nonNull(uriComponents.getHost()) ? uriComponents.getHost() : "")
            .putInt(Objects.nonNull(uriComponents.getPort()) ? uriComponents.getPort() : 0)
            .putUnencodedChars(
                Objects.nonNull(uriComponents.getPath()) ? uriComponents.getPath() : "")
            .putUnencodedChars(
                Objects.nonNull(uriComponents.getQuery()) ? uriComponents.getQuery() : "")
            .putUnencodedChars(
                Objects.nonNull(uriComponents.getFragment()) ? uriComponents.getFragment() : "");

    if (duo != null) {
      for (final String s : duo) {
        hasher.putUnencodedChars(s);
      }
    } else {
      duo = Collections.emptySortedSet();
    }

    return new ExternalReference(url, hasher.hash().toString(), duo);
  }

  @JsonCreator
  public static ExternalReference build(@JsonProperty("url") final String url) {
    return build(url, new TreeSet<>());
  }
}
