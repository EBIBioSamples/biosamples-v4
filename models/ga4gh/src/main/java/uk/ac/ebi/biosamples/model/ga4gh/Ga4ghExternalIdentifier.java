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

@JsonInclude(JsonInclude.Include.ALWAYS)
public class Ga4ghExternalIdentifier implements Comparable {
  private String identifier;
  private String relation;

  @JsonProperty("identifier")
  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  @JsonProperty("relation")
  public String getRelation() {
    return relation;
  }

  public void setRelation(String relation) {
    this.relation = relation;
  }

  @Override
  public int compareTo(Object o) {
    return this.getIdentifier().compareTo(((Ga4ghExternalIdentifier) o).getIdentifier());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Ga4ghExternalIdentifier that = (Ga4ghExternalIdentifier) o;
    return Objects.equals(identifier, that.identifier) && Objects.equals(relation, that.relation);
  }

  @Override
  public int hashCode() {

    return Objects.hash(identifier, relation);
  }

  @Override
  protected Ga4ghExternalIdentifier clone() {
    try {
      return (Ga4ghExternalIdentifier) super.clone();
    } catch (CloneNotSupportedException e) {
      return new Ga4ghExternalIdentifier();
    }
  }
}
