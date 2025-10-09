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
package uk.ac.ebi.biosamples.core.model.structured;

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

  StructuredTable(
      final URI schema,
      final String domain,
      final String webinSubmissionAccountId,
      final StructuredDataType type,
      final Set<T> entries) {
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
    return entries.stream()
        .map(StructuredEntry::getDataAsMap)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  public int compareTo(final AbstractData o) {
    if (o == null) {
      return 1;
    }

    if (!(o instanceof StructuredTable)) {
      return 1;
    }

    final StructuredTable other = (StructuredTable) o;
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
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof StructuredTable) {
      final StructuredTable<T> other = (StructuredTable<T>) o;
      return getDataType().equals(other.getDataType())
          && getSchema().equals(other.getSchema())
          && getDomain().equals(other.getDomain())
          && getWebinSubmissionAccountId().equals(other.getWebinSubmissionAccountId());
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(schema, entries, domain, webinSubmissionAccountId);
  }

  public static class Builder<T extends StructuredEntry> {
    private final URI schema;
    private final Set<T> entries;
    private final String domain;
    private final String webinSubmissionAccountId;
    private final StructuredDataType type;

    @JsonCreator
    public Builder(
        final URI schema,
        final String domain,
        final String webinSubmissionAccountId,
        final StructuredDataType type) {
      this.schema = schema;
      this.domain = domain;
      this.webinSubmissionAccountId = webinSubmissionAccountId;
      this.type = type;
      entries = new HashSet<>();
    }

    @JsonCreator
    public Builder(
        final String schema,
        final String domain,
        final String webinSubmissionAccountId,
        final StructuredDataType type) {
      this(URI.create(schema), domain, webinSubmissionAccountId, type);
    }

    @JsonProperty
    public Builder<T> addEntry(final T entry) {
      entries.add(entry);
      return this;
    }

    @JsonProperty
    public Builder<T> withEntries(final Collection<T> entries) {
      this.entries.addAll(entries);
      return this;
    }

    public StructuredTable<T> build() {
      return new StructuredTable<>(schema, domain, webinSubmissionAccountId, type, entries);
    }
  }
}
