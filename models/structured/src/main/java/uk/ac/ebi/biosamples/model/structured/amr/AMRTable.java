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
package uk.ac.ebi.biosamples.model.structured.amr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.*;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.StructuredCell;
import uk.ac.ebi.biosamples.model.structured.StructuredDataType;

// @JsonDeserialize(builder = AMRTable.Builder.class)
public class AMRTable extends AbstractData implements Comparable<AbstractData> {
  private final URI schema;
  private final Set<AMREntry> amrEntries;
  private final String domain;
  private final String webinSubmissionAccountId;

  public AMRTable(
      URI schema, Set<AMREntry> amrEntries, String domain, String webinSubmissionAccountId) {
    this.schema = schema;
    this.amrEntries = amrEntries;
    this.domain = domain;
    this.webinSubmissionAccountId = webinSubmissionAccountId;
  }

  @Override
  public String getDomain() {
    return domain;
  }

  @Override
  public String getWebinSubmissionAccountId() {
    return webinSubmissionAccountId;
  }

  @Override
  public StructuredDataType getDataType() {
    return StructuredDataType.AMR;
  }

  @Override
  public URI getSchema() {
    return schema;
  }

  @Override
  public Set<AMREntry> getStructuredData() {
    return amrEntries;
  }

  @Override
  public List<String> getHeaders() {
    return null;
  }

  @Override
  public List<Map<String, StructuredCell>> getDataAsMap() {
    return null;
  }

  @Override
  public int compareTo(AbstractData other) {
    if (other == null) {
      return 1;
    }

    if (!(other instanceof AMRTable)) {
      return 1;
    }

    int comparison;

    AMRTable otherAmrTable = (AMRTable) other;
    Set<AMREntry> otherTableAMREntries = otherAmrTable.getStructuredData();
    for (AMREntry entry : this.getStructuredData()) {
      Optional<AMREntry> otherEntry =
          otherTableAMREntries.parallelStream().filter(e -> e.equals(entry)).findFirst();
      if (!otherEntry.isPresent()) {
        return 1;
      } else {
        comparison = entry.compareTo(otherEntry.get());

        if (0 != comparison) {
          return comparison;
        }
      }
    }

    comparison = nullSafeStringComparison(this.schema.toString(), otherAmrTable.schema.toString());

    if (comparison != 0) {
      return comparison;
    }

    comparison = nullSafeStringComparison(this.domain, otherAmrTable.domain);

    if (comparison != 0) {
      return comparison;
    }

    return nullSafeStringComparison(
        this.webinSubmissionAccountId, otherAmrTable.webinSubmissionAccountId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AMRTable)) return false;
    AMRTable amrTable = (AMRTable) o;
    return Objects.equals(getSchema(), amrTable.getSchema())
        && Objects.equals(amrEntries, amrTable.amrEntries)
        && Objects.equals(getDomain(), amrTable.getDomain())
        && Objects.equals(getWebinSubmissionAccountId(), amrTable.getWebinSubmissionAccountId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getSchema(), amrEntries, domain, webinSubmissionAccountId);
  }

  private int nullSafeStringComparison(String one, String two) {

    if (one == null && two != null) {
      return -1;
    }
    if (one != null && two == null) {
      return 1;
    }
    if (one != null && !one.equals(two)) {
      return one.compareTo(two);
    }

    return 0;
  }

  public static class Builder {
    private URI schema;
    private Set<AMREntry> amrEntries;
    private String domain;
    private String webinSubmissionAccountId;

    @JsonCreator
    public Builder(URI schema, String domain, String webinSubmissionAccountId) {
      this.schema = schema;
      this.amrEntries = new HashSet<>();
      this.domain = domain;
      this.webinSubmissionAccountId = webinSubmissionAccountId;
    }

    @JsonCreator
    public Builder(String schema, String domain, String webinSubmissionAccountId) {
      this(URI.create(schema), domain, webinSubmissionAccountId);
    }

    @JsonProperty
    public Builder addEntry(AMREntry entry) {
      this.amrEntries.add(entry);
      return this;
    }

    @JsonProperty
    public Builder withEntries(Collection<AMREntry> entries) {
      this.amrEntries.addAll(entries);
      return this;
    }

    public AMRTable build() {
      return new AMRTable(this.schema, this.amrEntries, this.domain, this.webinSubmissionAccountId);
    }
  }
}
