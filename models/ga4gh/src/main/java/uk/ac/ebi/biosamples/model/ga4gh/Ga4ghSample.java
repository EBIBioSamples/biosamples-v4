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

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@JsonInclude
@JsonPropertyOrder({
  "id",
  "dataset_id",
  "individual_id",
  "name",
  "description",
  "bio_characteristic",
  "attributes",
  "external_identifiers",
  "individual_age_at_collection",
  "location"
})
public class Ga4ghSample {

  private String id;
  private String dataset_id;
  private String individual_id;
  private String name;
  private String description;
  private SortedSet<Ga4ghBiocharacteristics> bio_characteristic;
  private Ga4ghAttributes attributes;
  private SortedSet<Ga4ghExternalIdentifier> external_identifiers;
  private Ga4ghAge individual_age_at_collection;
  private Ga4ghGeoLocation location;

  @Autowired
  public Ga4ghSample(Ga4ghAttributes attributes) {
    this.attributes = attributes;
    bio_characteristic = new TreeSet<>();
    external_identifiers = new TreeSet<>();
  }

  @JsonCreator
  public static Ga4ghSample build(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("dataset_id") String dataset_id,
      @JsonProperty("individual_id") String individual_id,
      @JsonProperty("description") String description,
      @JsonProperty("biocharacteristic") Collection<Ga4ghBiocharacteristics> biocharacteristics,
      @JsonProperty("attributes") @JsonDeserialize(using = AttributeDeserializer.class)
          Ga4ghAttributes attributes,
      @JsonProperty("external_identifiers") Collection<Ga4ghExternalIdentifier> externalIdentifiers,
      @JsonProperty("individual_age_at_collection") Ga4ghAge age,
      @JsonProperty("location") Ga4ghGeoLocation location) {
    Ga4ghSample ga4ghSample = new Ga4ghSample(new Ga4ghAttributes());

    if (id == null) throw new IllegalArgumentException("Sample id must be provided");
    ga4ghSample.id = id.trim();

    if (name == null) throw new IllegalArgumentException("Sample id must be provided");
    ga4ghSample.name = name.trim();

    if (dataset_id != null) {
      ga4ghSample.dataset_id = dataset_id;
    }

    if (individual_id != null) {
      ga4ghSample.individual_id = individual_id;
    }

    if (description != null) {
      ga4ghSample.description = description;
    }

    if (biocharacteristics != null) {

      ga4ghSample.bio_characteristic.addAll(biocharacteristics);
    }

    if (attributes != null) {
      ga4ghSample.attributes = attributes;
    }

    if (externalIdentifiers != null) {
      ga4ghSample.external_identifiers.addAll(externalIdentifiers);
    }

    if (age != null) {
      ga4ghSample.individual_age_at_collection = age;
    }

    if (location != null) {
      ga4ghSample.location = location;
    }

    return ga4ghSample;
  }

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

  @JsonProperty("dataset_id")
  public String getDataset_id() {
    return dataset_id;
  }

  public void setDataset_id(String dataset_id) {
    this.dataset_id = dataset_id;
  }

  @JsonProperty("individual_id")
  public String getIndividual_id() {
    return individual_id;
  }

  public void setIndividual_id(String individual_id) {
    this.individual_id = individual_id;
  }

  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @JsonProperty("bio_characteristics")
  public SortedSet<Ga4ghBiocharacteristics> getBio_characteristic() {
    return bio_characteristic;
  }

  @JsonIgnore
  public String getReleasedDate() {
    SortedMap<String, List<AttributeValue>> values = attributes.getAttributes();
    List<AttributeValue> value = values.get("released");
    AttributeValue releasedDate = value.get(0); // because there will be only one attribute
    return (String) releasedDate.getValue();
  }

  @JsonIgnore
  public String getUpdatedDate() {
    SortedMap<String, List<AttributeValue>> values = attributes.getAttributes();
    List<AttributeValue> value = values.get("updated");
    AttributeValue releasedDate = value.get(0); // because there will be only one attribute
    return (String) releasedDate.getValue();
  }

  public void setBio_characteristic(SortedSet<Ga4ghBiocharacteristics> bio_characteristic) {
    this.bio_characteristic = bio_characteristic;
  }

  @JsonSerialize(using = AttributeSerializer.class)
  public Ga4ghAttributes getAttributes() {
    return attributes;
  }

  public void setAttributes(Ga4ghAttributes attributes) {
    this.attributes = attributes;
  }

  @JsonProperty("external_identifiers")
  public SortedSet<Ga4ghExternalIdentifier> getExternal_identifiers() {
    return external_identifiers;
  }

  public void setExternal_identifiers(SortedSet<Ga4ghExternalIdentifier> external_identifiers) {
    this.external_identifiers = external_identifiers;
  }

  @JsonProperty("individual_age_at_collection")
  public Ga4ghAge getIndividual_age_at_collection() {
    return individual_age_at_collection;
  }

  public void setIndividual_age_at_collection(Ga4ghAge individual_age_at_collection) {
    this.individual_age_at_collection = individual_age_at_collection;
  }

  @JsonProperty("location")
  public Ga4ghGeoLocation getLocation() {
    return location;
  }

  public void setLocation(Ga4ghGeoLocation location) {
    this.location = location;
  }

  public void addAttributeList(String label, List<AttributeValue> value) {
    attributes.addAttribute(label, value);
  }

  public void addSingleAttributeValue(String label, Object value) {
    List<AttributeValue> values = new ArrayList<>();
    values.add(new AttributeValue(value));
    addAttributeList(label, values);
  }

  public void addBioCharacteristic(Ga4ghBiocharacteristics biocharacteristics) {
    bio_characteristic.add(biocharacteristics);
  }

  @Override
  public Ga4ghSample clone() {
    Ga4ghSample ga4ghSample = null;
    try {
      ga4ghSample = (Ga4ghSample) super.clone();
    } catch (CloneNotSupportedException e) {
      ga4ghSample = new Ga4ghSample(new Ga4ghAttributes());
    }
    return ga4ghSample;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Ga4ghSample ga4ghSample = (Ga4ghSample) o;
    return Objects.equals(id, ga4ghSample.id)
        && Objects.equals(dataset_id, ga4ghSample.dataset_id)
        && Objects.equals(individual_id, ga4ghSample.individual_id)
        && Objects.equals(name, ga4ghSample.name)
        && Objects.equals(description, ga4ghSample.description)
        && Objects.equals(bio_characteristic, ga4ghSample.bio_characteristic)
        && Objects.equals(attributes, ga4ghSample.attributes)
        && Objects.equals(external_identifiers, ga4ghSample.external_identifiers)
        && Objects.equals(individual_age_at_collection, ga4ghSample.individual_age_at_collection)
        && Objects.equals(location, ga4ghSample.location);
  }

  @Override
  public int hashCode() {

    return Objects.hash(
        id,
        dataset_id,
        individual_id,
        name,
        description,
        bio_characteristic,
        attributes,
        external_identifiers,
        individual_age_at_collection,
        location);
  }
}
