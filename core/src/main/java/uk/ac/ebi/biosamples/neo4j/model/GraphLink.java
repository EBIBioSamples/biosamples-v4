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
import uk.ac.ebi.biosamples.core.model.RelationshipType;

public class GraphLink implements Comparable<GraphLink> {
  private RelationshipType type;
  private String startNode;
  private String endNode;

  public RelationshipType getType() {
    return type;
  }

  public void setType(final RelationshipType type) {
    this.type = type;
  }

  private String getStartNode() {
    return startNode;
  }

  public void setStartNode(final String startNode) {
    this.startNode = startNode;
  }

  private String getEndNode() {
    return endNode;
  }

  public void setEndNode(final String endNode) {
    this.endNode = endNode;
  }

  public String getQueryString(final String relName) {
    final String rel = (type == RelationshipType.ANY) ? relName : relName + ":" + type;
    return "(" + startNode + ")-[" + rel + "]->(" + endNode + ") ";
  }

  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof GraphLink)) {
      return false;
    }
    final GraphLink otherLink = (GraphLink) other;
    return type.equals(otherLink.getType())
        && startNode.equals(otherLink.getStartNode())
        && endNode.equals(otherLink.getEndNode());
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, startNode, endNode);
  }

  @Override
  public int compareTo(final GraphLink other) {
    if (!type.equals(other.getType())) {
      return type.compareTo(other.getType());
    }

    if (!startNode.equals(other.getStartNode())) {
      return startNode.compareTo(other.getStartNode());
    }

    return endNode.compareTo(other.getEndNode());
  }
}
