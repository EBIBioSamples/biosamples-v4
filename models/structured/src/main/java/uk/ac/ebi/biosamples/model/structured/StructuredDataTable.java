package uk.ac.ebi.biosamples.model.structured;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

  public static StructuredDataTable build(String domain,
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
    if (domain != null && !domain.isEmpty() && other.getDomain() != null && !other.getDomain().isEmpty()) {
      if (!domain.equals(other.getDomain())) {
        return false;
      }
    } else if (webinSubmissionAccountId != null && !webinSubmissionAccountId.isEmpty()
               && other.getWebinSubmissionAccountId() != null && !other.getWebinSubmissionAccountId().isEmpty()) {
      if (!webinSubmissionAccountId.equals(other.getWebinSubmissionAccountId())) {
        return false;
      }
    } else {
      return false;
    }

    return type.equalsIgnoreCase(other.type);
  }
}
