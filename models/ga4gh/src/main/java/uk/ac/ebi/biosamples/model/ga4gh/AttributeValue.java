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
package uk.ac.ebi.biosamples.model.ga4gh;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import java.util.Objects;

@JsonSerialize(using = AttributeValueSerializer.class)
public class AttributeValue {

  private String type;
  private Object value;

  public AttributeValue() {}

  public AttributeValue(Object value) {
    if (value instanceof String) {
      type = "string_value";
      this.value = value;
    } else if (value instanceof Long) {
      type = "int64_value";
      this.value = value;
    } else if (value instanceof Boolean) {
      type = "bool_value";
      this.value = value;
    } else if (value instanceof Double) {
      type = "double_value";
      this.value = value;
    } else if (value instanceof Ga4ghExternalIdentifier) {
      type = "external_identifier";
      this.value = value;
    } else if (value instanceof Ga4ghOntologyTerm) {
      type = "ontology_term";
      this.value = value;
    } else if (value instanceof Ga4ghExperiment) {
      type = "experiment";
      this.value = value;
    } else if (value instanceof Analysis) {
      type = "analysis";
      this.value = value;
    } else if (value == null) {
      type = "null_value";
    } else if (value instanceof Ga4ghAttributes) {
      type = "attributes";
      this.value = value;
    } else if (isListOfAttributes(value)) {
      type = "attribute_list";
      this.value = value;
    } else {
      throw new TypeNotPresentException("Type is not supported", new Exception());
    }
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  private boolean isListOfAttributes(Object value) {
    try {
      List<AttributeValue> attributeValues = (List<AttributeValue>) value;
      return true;
    } catch (ClassCastException e) {
      return false;
    }
  }

  @JsonCreator
  public static AttributeValue build() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AttributeValue that = (AttributeValue) o;
    return Objects.equals(type, that.type) && Objects.equals(value, that.value);
  }

  @Override
  protected AttributeValue clone() {
    try {
      return (AttributeValue) super.clone();
    } catch (CloneNotSupportedException e) {
      return new AttributeValue();
    }
  }
}
