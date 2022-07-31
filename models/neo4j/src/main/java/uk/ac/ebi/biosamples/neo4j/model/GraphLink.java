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

import java.util.Objects;
import uk.ac.ebi.biosamples.model.RelationshipType;

public class GraphLink implements Comparable<GraphLink> {
  private RelationshipType type;
  private String startNode;
  private String endNode;

  public RelationshipType getType() {
    return type;
  }

  public void setType(RelationshipType type) {
    this.type = type;
  }

  public String getStartNode() {
    return startNode;
  }

  public void setStartNode(String startNode) {
    this.startNode = startNode;
  }

  public String getEndNode() {
    return endNode;
  }

  public void setEndNode(String endNode) {
    this.endNode = endNode;
  }

  public String getQueryString(String relName) {
    String rel = (type == RelationshipType.ANY) ? relName : relName + ":" + type;
    return "(" + startNode + ")-[" + rel + "]->(" + endNode + ") ";
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof GraphLink)) {
      return false;
    }
    GraphLink otherLink = (GraphLink) other;
    return this.type.equals(otherLink.getType())
        && this.startNode.equals(otherLink.getStartNode())
        && this.endNode.equals(otherLink.getEndNode());
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, startNode, endNode);
  }

  @Override
  public int compareTo(GraphLink other) {
    if (!this.type.equals(other.getType())) {
      return this.type.compareTo(other.getType());
    }

    if (!this.startNode.equals(other.getStartNode())) {
      return this.startNode.compareTo(other.getStartNode());
    }

    return this.endNode.compareTo(other.getEndNode());
  }
}
