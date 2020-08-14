/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.legacy.json.domain;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"text", "ontologyTerms"})
public class LegacyAttribute {

  private uk.ac.ebi.biosamples.model.Attribute attribute;

  public LegacyAttribute(uk.ac.ebi.biosamples.model.Attribute attribute) {
    this.attribute = attribute;
  }

  @JsonGetter
  public String text() {
    return attribute.getValue();
  }

  public String type() {
    return attribute.getType();
  }

  @JsonGetter
  public String[] ontologyTerms() {
    if (hasOntologyTerm()) {
      return attribute.getIri().toArray(new String[attribute.getIri().size()]);
    } else {
      return null;
    }
  }

  @JsonIgnore
  private boolean hasOntologyTerm() {
    return attribute.getIri().size() > 0;
  }
}
