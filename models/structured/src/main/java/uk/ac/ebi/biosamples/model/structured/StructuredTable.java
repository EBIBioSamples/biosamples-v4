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
package uk.ac.ebi.biosamples.model.structured;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class StructuredTable<T extends StructuredEntry> extends AbstractData
    implements Comparable<AbstractData> {
  private final URI schema;
  private final String domain;
  private final String webinSubmissionAccountId;
  private final StructuredDataType type;
  private final Set<T> entries;

  public StructuredTable(
      URI schema,
      String domain,
      String webinSubmissionAccountId,
      StructuredDataType type,
      Set<T> entries) {
    this.schema = schema;
    this.domain = domain;
    this.webinSubmissionAccountId = webinSubmissionAccountId;
    this.type = type;
    this.entries = entries;
  }

  @Override
  public URI getSchema() {
    return schema;
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
    return type;
  }

  @Override
  public Set<T> getStructuredData() {
    return entries;
  }

  @Override
  public List<String> getHeaders() {
    return type.getHeaders();
  }

  @Override
  public List<Map<String, StructuredCell>> getDataAsMap() {
    return entries.stream().map(StructuredEntry::getDataAsMap).filter(Objects::nonNull).collect(Collectors.toList());
  }

  @Override
  public int compareTo(AbstractData o) {
    if (o == null) {
      return 1;
    }

    if (!(o instanceof StructuredTable)) {
      return 1;
    }

    StructuredTable other = (StructuredTable) o;
    int cmp = type.compareTo(other.type);
    if (cmp != 0) {
      return cmp;
    }

    if (domain != null) {
      cmp = domain.compareTo(other.domain);
      if (cmp != 0) {
        return cmp;
      }
    }

    if (webinSubmissionAccountId != null) {
      cmp = webinSubmissionAccountId.compareTo(other.webinSubmissionAccountId);
      if (cmp != 0) {
        return cmp;
      }
    }
    return schema.compareTo(other.schema);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof StructuredTable) {
      StructuredTable<T> other = (StructuredTable<T>) o;
      return this.getDataType().equals(other.getDataType())
          && this.getSchema().equals(other.getSchema())
          && this.getDomain().equals(other.getDomain())
          && this.getWebinSubmissionAccountId().equals(other.getWebinSubmissionAccountId());
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(schema, entries, domain, webinSubmissionAccountId);
  }

  public static class Builder<T extends StructuredEntry> {
    private URI schema;
    private Set<T> entries;
    private String domain;
    private String webinSubmissionAccountId;
    private StructuredDataType type;

    @JsonCreator
    public Builder(
        URI schema, String domain, String webinSubmissionAccountId, StructuredDataType type) {
      this.schema = schema;
      this.domain = domain;
      this.webinSubmissionAccountId = webinSubmissionAccountId;
      this.type = type;
      this.entries = new HashSet<>();
    }

    @JsonCreator
    public Builder(
        String schema, String domain, String webinSubmissionAccountId, StructuredDataType type) {
      this(URI.create(schema), domain, webinSubmissionAccountId, type);
    }

    @JsonProperty
    public Builder<T> addEntry(T entry) {
      this.entries.add(entry);
      return this;
    }

    @JsonProperty
    public Builder<T> withEntries(Collection<T> entries) {
      this.entries.addAll(entries);
      return this;
    }

    public StructuredTable<T> build() {
      return new StructuredTable<>(
          this.schema, this.domain, this.webinSubmissionAccountId, this.type, this.entries);
    }
  }
}
