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
import java.util.List;

/**
 * Object to represent the ld+json version of the @see <a
 * href="http://schema.org/PropertyValue">Property Value</a> in schema.org
 */
@JsonPropertyOrder({"@type", "name", "value", "valueReference"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonLDPropertyValue implements BioschemasObject {

  @JsonProperty("@type")
  private final String type = "PropertyValue";

  private String name;
  private String value;
  private String unitText;
  private String unitCode;

  //    @JsonProperty("valueReference")
  //    private List<JsonLDStructuredValue> valueReference;

  private List<JsonLDDefinedTerm> valueReference;
  private List<JsonLDDefinedTerm> propertyId;

  public String getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public List<JsonLDDefinedTerm> getValueReference() {
    return valueReference;
  }

  public void setValueReference(List<JsonLDDefinedTerm> valueReference) {
    this.valueReference = valueReference;
  }

  public String getUnitCode() {
    return unitCode;
  }

  public JsonLDPropertyValue unitCode(String unitCode) {
    this.unitCode = unitCode;
    return this;
  }

  public String getUnitText() {
    return unitText;
  }

  public JsonLDPropertyValue unitText(String unitText) {
    this.unitText = unitText;
    return this;
  }
}
