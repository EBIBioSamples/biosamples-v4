package uk.ac.ebi.biosamples.curation;

public class TaxonEntry {
  private long taxId;
  private String ncbiTaxonName;
  private String bioSampleTaxName;
  private String bioSampleAccession;

  public long getTaxId() {
    return taxId;
  }

  public String getNcbiTaxonName() {
    return ncbiTaxonName;
  }

  public String getBioSampleTaxName() {
    return bioSampleTaxName;
  }

  public String getBioSampleAccession() {
    return bioSampleAccession;
  }
}
