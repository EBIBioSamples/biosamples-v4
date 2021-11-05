package uk.ac.ebi.biosamples.model.structured;

import java.time.Instant;
import java.util.Set;

public class StructuredData {
  protected String accession;
  protected Instant create;
  protected Instant update;
  protected Set<StructuredDataTable> data;

  protected StructuredData() {
  }

  public String getAccession() {
    return accession;
  }

  public Instant getCreate() {
    return create;
  }

  public Instant getUpdate() {
    return update;
  }

  public Set<StructuredDataTable> getData() {
    return data;
  }

  public static StructuredData build(String accession, Instant create, Set<StructuredDataTable> data) {
    StructuredData structuredData = new StructuredData();
    structuredData.accession = accession;
    structuredData.create = create;
    structuredData.update = Instant.now();
    structuredData.data = data;
    return structuredData;
  }

  public static StructuredData build(String accession, Instant create, Instant update, Set<StructuredDataTable> data) {
    StructuredData structuredData = new StructuredData();
    structuredData.accession = accession;
    structuredData.create = create;
    structuredData.update = update;
    structuredData.data = data;
    return structuredData;
  }
}
