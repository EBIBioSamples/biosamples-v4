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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class StructuredDataTable {
  private String domain;
  private String webinSubmissionAccountId;
  private String type;
  private String schema;
  private Set<Map<String, StructuredDataEntry>> content;

  public String getDomain() {
    return domain;
  }

  public String getWebinSubmissionAccountId() {
    return webinSubmissionAccountId;
  }

  public String getType() {
    return type;
  }

  public String getSchema() {
    return schema;
  }

  public Set<Map<String, StructuredDataEntry>> getContent() {
    return content;
  }

  public static StructuredDataTable build(
      String domain,
      String webinSubmissionAccountId,
      String type,
      String schema,
      Set<Map<String, StructuredDataEntry>> content) {
    StructuredDataTable structuredDataTable = new StructuredDataTable();
    structuredDataTable.schema = schema;
    structuredDataTable.domain = domain;
    structuredDataTable.webinSubmissionAccountId = webinSubmissionAccountId;
    structuredDataTable.type = type;
    structuredDataTable.content = content;

    return structuredDataTable;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof StructuredDataTable) {
      return hasSimilarData((StructuredDataTable) o);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(domain, webinSubmissionAccountId, type);
  }

  public boolean hasSimilarData(StructuredDataTable other) {
    if (domain != null && !domain.isEmpty()
        && other.getDomain() != null && !other.getDomain().isEmpty()) {
      if (!domain.equals(other.getDomain())) {
        return false;
      }
    } else if (webinSubmissionAccountId != null && !webinSubmissionAccountId.isEmpty()
        && other.getWebinSubmissionAccountId() != null && !other.getWebinSubmissionAccountId().isEmpty()) {
      if (!webinSubmissionAccountId.equals(other.getWebinSubmissionAccountId())) {
        return false;
      }
    } else if (webinSubmissionAccountId == null && other.getWebinSubmissionAccountId() == null &&
        domain == null && other.getDomain() == null) {
      return type.equalsIgnoreCase(other.type);
    } else {
      return false;
    }

    return type.equalsIgnoreCase(other.type);
  }

  @JsonIgnore
  public SortedSet<String> getHeaders() {
    SortedSet<String> headers = new TreeSet<>();
    content.forEach(row -> headers.addAll(row.keySet()));
    return headers;
  }
}
