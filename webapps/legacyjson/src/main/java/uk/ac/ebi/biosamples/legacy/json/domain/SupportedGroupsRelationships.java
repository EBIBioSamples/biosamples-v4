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

public enum SupportedGroupsRelationships {
  SAMPLES("samples"),
  EXTERNAL_LINKS("externallinks");

  private String relationshipName;

  SupportedGroupsRelationships(String name) {
    this.relationshipName = name;
  }

  public String getRelationshipName() {
    return this.relationshipName;
  }

  public static SupportedGroupsRelationships getFromName(String name) {
    for (SupportedGroupsRelationships rel : values()) {
      if (rel.getRelationshipName().equals(name)) return rel;
    }

    return null;
  }
}
