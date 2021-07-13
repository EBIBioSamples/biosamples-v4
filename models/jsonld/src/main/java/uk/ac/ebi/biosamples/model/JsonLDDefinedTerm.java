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
package uk.ac.ebi.biosamples.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"@id", "@type", "name", "inDefinedTermSet", "termCode"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonLDDefinedTerm implements BioschemasObject {

  @JsonProperty("@type")
  private final String type = "DefinedTerm";

  @JsonProperty("@id")
  private String id;

  private String name;
  private String inDefinedTermSet;
  private String termCode;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public String getInDefinedTermSet() {
    return inDefinedTermSet;
  }

  public String getTermCode() {
    return termCode;
  }

  public void setTermCode(String termCode) {
    this.termCode = termCode;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setInDefinedTermSet(String inDefinedTermSet) {
    this.inDefinedTermSet = inDefinedTermSet;
  }
}
