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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
@JsonSerialize(using = AttributeSerializer.class)
@JsonDeserialize(using = AttributeDeserializer.class)
public class Ga4ghAttributes {
  private SortedMap<String, List<AttributeValue>> attributes;

  public Ga4ghAttributes() {
    this.attributes = new TreeMap<>();
  }

  public SortedMap<String, List<AttributeValue>> getAttributes() {
    return attributes;
  }

  public void setAttributes(SortedMap<String, List<AttributeValue>> attributes) {
    this.attributes = attributes;
  }

  void addAttribute(String label, List<AttributeValue> values) {
    try {
      attributes.put(label, values);
    } catch (NullPointerException e) {
      System.out.println();
    }
  }

  public void addSingleAttribute(String label, AttributeValue value) {
    ArrayList<AttributeValue> values = new ArrayList<>();
    values.add(value);
    attributes.put(label, values);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Ga4ghAttributes that = (Ga4ghAttributes) o;
    return Objects.equals(attributes, that.attributes);
  }

  @Override
  public int hashCode() {

    return Objects.hash(attributes);
  }

  @Override
  protected Ga4ghAttributes clone() {
    try {
      return (Ga4ghAttributes) super.clone();
    } catch (CloneNotSupportedException e) {
      return new Ga4ghAttributes();
    }
  }
}
