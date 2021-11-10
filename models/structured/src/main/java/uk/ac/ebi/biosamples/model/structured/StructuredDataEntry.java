package uk.ac.ebi.biosamples.model.structured;

public class StructuredDataEntry {
  private String value;
  private String iri;

  public String getValue() {
    return value;
  }

  public String getIri() {
    return iri;
  }

  public static StructuredDataEntry build(String value,
                                          String iri) {
    StructuredDataEntry structuredDataEntry = new StructuredDataEntry();
    structuredDataEntry.value = value;
    structuredDataEntry.iri = iri;

    return structuredDataEntry;
  }
}
