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

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class GraphNode implements Comparable<GraphNode> {
  private String id;
  private String type;
  private Map<String, String> attributes;

  public GraphNode() {
    // default constructor
  }

  public GraphNode(Map<String, Object> nodeAsMap) {
    type = String.valueOf(nodeAsMap.get("type"));
    attributes = (Map<String, String>) nodeAsMap.get("attributes");
    id = attributes.get("accession");
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public String getQueryString() {
    String queryString;
    if (attributes != null) {
      StringJoiner joiner = new StringJoiner(",");
      for (Map.Entry<String, String> e : attributes.entrySet()) {
        joiner.add(
            e.getKey().replaceAll("\\s+", "").toLowerCase()
                + ":'"
                + e.getValue().toLowerCase()
                + "'");
      }
      queryString = ":" + type + "{" + joiner.toString() + "}";
    } else {
      queryString = ":" + type;
    }
    return queryString;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof GraphNode && this.type.equalsIgnoreCase(((GraphNode) other).getType())) {
      return this.id.equals(((GraphNode) other).id);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type.toUpperCase(), id);
  }

  @Override
  public int compareTo(GraphNode other) {
    if (this.type.equalsIgnoreCase(other.getType())) {
      return this.id.compareTo(other.getId());
    } else {
      return this.type.compareTo(other.getType());
    }
  }
}
