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
package uk.ac.ebi.biosamples.model.structured.amr;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class AmrPair implements Comparable<AmrPair> {
  String value;
  String iri;

  public AmrPair() {}

  public AmrPair(String value) {
    this.value = value;
  }

  public AmrPair(String value, String iri) {
    this.value = value;
    this.iri = iri;
  }

  @Override
  public String toString() {
    return "antibiotic_name{" + "value='" + value + '\'' + ", iri='" + iri + '\'' + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AmrPair)) return false;
    AmrPair amrPairs = (AmrPair) o;
    return Objects.equals(getValue(), amrPairs.getValue())
        && Objects.equals(getIri(), amrPairs.getIri());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getValue(), getIri());
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
  public int compareTo(AmrPair o) {
    return this.getValue().compareTo(o.getValue());
  }
}
