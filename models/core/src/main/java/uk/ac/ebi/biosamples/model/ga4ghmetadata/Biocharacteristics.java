package com.example.simple_biosamples_client.models.ga4ghmetadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class Biocharacteristics implements Comparable {

    private String description;
    private SortedSet<OntologyTerm> ontology_terms;
    private SortedSet<OntologyTerm> negated_ontology_terms;
    private String scope;

    public Biocharacteristics() {
        ontology_terms = new TreeSet<>();
        negated_ontology_terms = new TreeSet<>();
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("ontology_terms")
    public SortedSet<OntologyTerm> getOntology_terms() {
        return ontology_terms;
    }

    public void setOntology_terms(SortedSet<OntologyTerm> ontology_terms) {
        this.ontology_terms = ontology_terms;
    }

    @JsonProperty("negated_ontology_terms")
    public SortedSet<OntologyTerm> getNegated_ontology_terms() {
        return negated_ontology_terms;
    }

    public void setNegated_ontology_terms(SortedSet<OntologyTerm> negated_ontology_terms) {
        this.negated_ontology_terms = negated_ontology_terms;
    }

    @JsonProperty("scope")
    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }


    @Override
    public int compareTo(Object o) {
        return this.getDescription().compareTo(((Biocharacteristics) o).getDescription());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Biocharacteristics that = (Biocharacteristics) o;
        return Objects.equals(description, that.description) &&
                Objects.equals(ontology_terms, that.ontology_terms) &&
                Objects.equals(negated_ontology_terms, that.negated_ontology_terms) &&
                Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {

        return Objects.hash(description, ontology_terms, negated_ontology_terms, scope);
    }
}
