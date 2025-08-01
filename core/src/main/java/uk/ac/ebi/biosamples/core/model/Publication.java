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
package uk.ac.ebi.biosamples.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = Publication.Builder.class)
public class Publication implements Comparable<Publication> {
  private final String doi;
  private final String pubmed_id;

  private Publication(final String doi, final String pubmed_id) {
    this.doi = doi;
    this.pubmed_id = pubmed_id;
  }

  @JsonProperty("doi")
  public String getDoi() {
    return doi;
  }

  @JsonProperty("pubmed_id")
  public String getPubMedId() {
    return pubmed_id;
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }
    if (!(o instanceof Publication)) {
      return false;
    }
    final Publication other = (Publication) o;
    return Objects.equals(doi, other.doi) && Objects.equals(pubmed_id, other.pubmed_id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(doi, pubmed_id);
  }

  @Override
  public int compareTo(final Publication other) {
    if (other == null) {
      return 1;
    }

    final int comparisonResult = nullSafeStringComparison(doi, other.doi);
    if (comparisonResult != 0) {
      return comparisonResult;
    }

    return nullSafeStringComparison(pubmed_id, other.pubmed_id);
  }

  @Override
  public String toString() {
    return "Publication{" + "doi='" + doi + '\'' + ", pubmed_id='" + pubmed_id + '\'' + '}';
  }

  private int nullSafeStringComparison(final String first, final String other) {
    if (first == null && other == null) {
      return 0;
    }
    if (first == null) {
      return -1;
    }
    if (other == null) {
      return 1;
    }
    return first.compareTo(other);
  }

  //	@JsonCreator
  //	public static Publication build(@JsonProperty("doi") String doi,
  //			@JsonProperty("pubmed_id") String pubmedId) {
  //		return new Publication(doi, pubmedId);
  //	}

  public static class Builder {
    private String doi;
    private String pubmed_id;

    @JsonCreator
    public Builder() {}

    @JsonProperty("doi")
    public Builder doi(final String doi) {
      this.doi = doi;
      return this;
    }

    @JsonProperty("pubmed_id")
    public Builder pubmed_id(final String pubmed_id) {
      this.pubmed_id = pubmed_id;
      return this;
    }

    public Publication build() {
      return new Publication(doi, pubmed_id);
    }
  }
}
