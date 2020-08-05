package uk.ac.ebi.biosamples.model.structured;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.ebi.biosamples.utils.StringUtils;

import java.util.Objects;

public class StructuredCell implements Comparable<StructuredCell> {
    private String value;
    private String iri;

    public StructuredCell() {
        //emtpy constructor for jackson
    }

    public StructuredCell(String value, String iri) {
        this.value = value;
        this.iri = iri;
    }

    @JsonProperty("value")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @JsonProperty("iri")
    public String getIri() {
        return iri;
    }

    public void setIri(String iri) {
        this.iri = iri;
    }

    @Override
    public String toString() {
        return "{" +
                "value='" + value + "'," +
                "iri='" + iri + "'" +
                "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof StructuredCell) {
            StructuredCell other = (StructuredCell) o;
            return Objects.equals(this.getValue(), other.getValue()) && Objects.equals(this.getIri(), other.getIri());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue(), getIri());
    }

    @Override
    public int compareTo(StructuredCell other) {
        int cmp = StringUtils.nullSafeStringComparison(this.getValue(), other.getValue());
        if (cmp != 0) {
            return cmp;
        }
        return StringUtils.nullSafeStringComparison(this.getIri(), other.getIri());
    }
}
