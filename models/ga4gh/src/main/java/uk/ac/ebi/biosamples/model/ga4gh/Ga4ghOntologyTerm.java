package uk.ac.ebi.biosamples.model.ga4gh;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude
public class Ga4ghOntologyTerm implements Comparable {
    private String term_id;
    private String term_label;
    private String url;

    @JsonProperty("term_id")
    public String getTerm_id() {
        return term_id;
    }

    public void setTerm_id(String term_id) {
        this.term_id = term_id;
    }

    @JsonProperty("term_label")
    public String getTerm_label() {
        return term_label;
    }

    public void setTerm_label(String term_label) {
        this.term_label = term_label;
    }

    @JsonIgnore
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public int compareTo(Object o) {
        return this.getTerm_label().compareTo(((Ga4ghOntologyTerm) o).getTerm_label());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ga4ghOntologyTerm term = (Ga4ghOntologyTerm) o;
        return Objects.equals(term_id, term.term_id) &&
                Objects.equals(term_label, term.term_label);
    }

    @Override
    public int hashCode() {

        return Objects.hash(term_id, term_label);
    }

    @Override
    protected Ga4ghOntologyTerm clone(){
        try {
            return (Ga4ghOntologyTerm) super.clone();
        }catch (CloneNotSupportedException e){
            return new Ga4ghOntologyTerm();
        }
    }
}
