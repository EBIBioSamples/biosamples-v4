package uk.ac.ebi.biosamples.ols;

public class OlsResult {
    private String label;
    private String iri;

    public OlsResult(String label, String iri) {
        this.label = label;
        this.iri = iri;
    }

    public String getLabel() {
        return label;
    }

    public String getIri() {
        return iri;
    }
}
