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
package uk.ac.ebi.biosamples.neo4j.model;

import uk.ac.ebi.biosamples.model.RelationshipType;

public class GraphRelationship {
  private RelationshipType type;
  private GraphNode startNode;
  private GraphNode endNode;

  public RelationshipType getType() {
    return type;
  }

  public void setType(RelationshipType type) {
    this.type = type;
  }

  public GraphNode getStartNode() {
    return startNode;
  }

  public void setStartNode(GraphNode startNode) {
    this.startNode = startNode;
  }

  public GraphNode getEndNode() {
    return endNode;
  }

  public void setEndNode(GraphNode endNode) {
    this.endNode = endNode;
  }

  public String getQueryString() {
    return "(a"
        + startNode.getQueryString()
        + ")-[r:"
        + type
        + "]-(b"
        + endNode.getQueryString()
        + ")";
  }
}
