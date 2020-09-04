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
package uk.ac.ebi.biosamples.neo4j.model;

import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.RelationshipType;

public class NeoRelationship {
  private RelationshipType type;
  private String source;
  private String target;

  private NeoRelationship(RelationshipType type, String source, String target) {
    this.type = type;
    this.source = source;
    this.target = target;
  }

  public RelationshipType getType() {
    return type;
  }

  public String getSource() {
    return source;
  }

  public String getTarget() {
    return target;
  }

  public static NeoRelationship build(Relationship relationship) {
    return new NeoRelationship(
        RelationshipType.getType(relationship.getType()),
        relationship.getSource(),
        relationship.getTarget());
  }
}
