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

import java.util.Objects;

public class Ga4ghExperiment {
  private String id;
  private String name;
  private String description;
  private String created;
  private String updated;
  private String run_time;
  private String molecule;
  private String strategy;
  private String selection;
  private String library;
  private String library_layout;
  private String unstrument_model;
  private String sequencing_center;
  private Ga4ghGeoLocation location;
  private String platform_util;
  private Ga4ghAttributes attributes;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCreated() {
    return created;
  }

  public void setCreated(String created) {
    this.created = created;
  }

  public String getUpdated() {
    return updated;
  }

  public void setUpdated(String updated) {
    this.updated = updated;
  }

  public String getRun_time() {
    return run_time;
  }

  public void setRun_time(String run_time) {
    this.run_time = run_time;
  }

  public String getMolecule() {
    return molecule;
  }

  public void setMolecule(String molecule) {
    this.molecule = molecule;
  }

  public String getStrategy() {
    return strategy;
  }

  public void setStrategy(String strategy) {
    this.strategy = strategy;
  }

  public String getSelection() {
    return selection;
  }

  public void setSelection(String selection) {
    this.selection = selection;
  }

  public String getLibrary() {
    return library;
  }

  public void setLibrary(String library) {
    this.library = library;
  }

  public String getLibrary_layout() {
    return library_layout;
  }

  public void setLibrary_layout(String library_layout) {
    this.library_layout = library_layout;
  }

  public String getUnstrument_model() {
    return unstrument_model;
  }

  public void setUnstrument_model(String unstrument_model) {
    this.unstrument_model = unstrument_model;
  }

  public String getSequencing_center() {
    return sequencing_center;
  }

  public void setSequencing_center(String sequencing_center) {
    this.sequencing_center = sequencing_center;
  }

  public Ga4ghGeoLocation getLocation() {
    return location;
  }

  public void setLocation(Ga4ghGeoLocation location) {
    this.location = location;
  }

  public String getPlatform_util() {
    return platform_util;
  }

  public void setPlatform_util(String platform_util) {
    this.platform_util = platform_util;
  }

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
    Ga4ghExperiment that = (Ga4ghExperiment) o;
    return Objects.equals(id, that.id)
        && Objects.equals(name, that.name)
        && Objects.equals(description, that.description)
        && Objects.equals(created, that.created)
        && Objects.equals(updated, that.updated)
        && Objects.equals(run_time, that.run_time)
        && Objects.equals(molecule, that.molecule)
        && Objects.equals(strategy, that.strategy)
        && Objects.equals(selection, that.selection)
        && Objects.equals(library, that.library)
        && Objects.equals(library_layout, that.library_layout)
        && Objects.equals(unstrument_model, that.unstrument_model)
        && Objects.equals(sequencing_center, that.sequencing_center)
        && Objects.equals(location, that.location)
        && Objects.equals(platform_util, that.platform_util)
        && Objects.equals(attributes, that.attributes);
  }

  @Override
  public int hashCode() {

    return Objects.hash(
        id,
        name,
        description,
        created,
        updated,
        run_time,
        molecule,
        strategy,
        selection,
        library,
        library_layout,
        unstrument_model,
        sequencing_center,
        location,
        platform_util,
        attributes);
  }

  @Override
  protected Ga4ghExperiment clone() {
    try {
      return (Ga4ghExperiment) super.clone();
    } catch (CloneNotSupportedException e) {
      return new Ga4ghExperiment();
    }
  }
}
