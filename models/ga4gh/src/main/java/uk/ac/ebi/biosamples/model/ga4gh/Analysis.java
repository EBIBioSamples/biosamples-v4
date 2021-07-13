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
import java.util.Arrays;
import java.util.Objects;

@JsonInclude
public class Analysis {
  private String id;
  private String name;
  private String description;
  private String created;
  private String updated;
  private String type;
  private String[] software;
  private Ga4ghAttributes attributes;

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @JsonProperty("created")
  public String getCreated() {
    return created;
  }

  public void setCreated(String created) {
    this.created = created;
  }

  @JsonProperty("updated")
  public String getUpdated() {
    return updated;
  }

  public void setUpdated(String updated) {
    this.updated = updated;
  }

  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @JsonProperty("software")
  public String[] getSoftware() {
    return software;
  }

  public void setSoftware(String[] software) {
    this.software = software;
  }

  @JsonProperty("attributes")
  public Ga4ghAttributes getAttributes() {
    return attributes;
  }

  public void setAttributes(Ga4ghAttributes attributes) {
    this.attributes = attributes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Analysis analysis = (Analysis) o;
    return Objects.equals(id, analysis.id)
        && Objects.equals(name, analysis.name)
        && Objects.equals(description, analysis.description)
        && Objects.equals(created, analysis.created)
        && Objects.equals(updated, analysis.updated)
        && Objects.equals(type, analysis.type)
        && Arrays.equals(software, analysis.software)
        && Objects.equals(attributes, analysis.attributes);
  }

  @Override
  public int hashCode() {

    int result = Objects.hash(id, name, description, created, updated, type, attributes);
    result = 31 * result + Arrays.hashCode(software);
    return result;
  }

  @Override
  protected Analysis clone() {
    try {
      return (Analysis) super.clone();
    } catch (CloneNotSupportedException e) {
      return new Analysis();
    }
  }
}
