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
package uk.ac.ebi.biosamples.model.ga4gh;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class Ga4ghBiocharacteristics implements Comparable {

  private String description;
  private SortedSet<Ga4ghOntologyTerm> ontology_terms;
  private SortedSet<Ga4ghOntologyTerm> negated_ontology_terms;
  private String scope;

  public Ga4ghBiocharacteristics() {
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
  public SortedSet<Ga4ghOntologyTerm> getOntology_terms() {
    return ontology_terms;
  }

  public void setOntology_terms(SortedSet<Ga4ghOntologyTerm> ontology_terms) {
    this.ontology_terms = ontology_terms;
  }

  @JsonProperty("negated_ontology_terms")
  public SortedSet<Ga4ghOntologyTerm> getNegated_ontology_terms() {
    return negated_ontology_terms;
  }

  public void setNegated_ontology_terms(SortedSet<Ga4ghOntologyTerm> negated_ontology_terms) {
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
    return this.getDescription().compareTo(((Ga4ghBiocharacteristics) o).getDescription());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Ga4ghBiocharacteristics that = (Ga4ghBiocharacteristics) o;
    return Objects.equals(description, that.description)
        && Objects.equals(ontology_terms, that.ontology_terms)
        && Objects.equals(negated_ontology_terms, that.negated_ontology_terms)
        && Objects.equals(scope, that.scope);
  }

  @Override
  public int hashCode() {

    return Objects.hash(description, ontology_terms, negated_ontology_terms, scope);
  }

  @Override
  protected Ga4ghBiocharacteristics clone() {
    try {
      return (Ga4ghBiocharacteristics) super.clone();
    } catch (CloneNotSupportedException e) {
      return new Ga4ghBiocharacteristics();
    }
  }
}
