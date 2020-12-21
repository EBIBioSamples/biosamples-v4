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

import java.util.Set;

public class GraphSearchQuery {
  private Set<GraphNode> nodes;
  private Set<GraphLink> links;
  private int page;
  private int size;
  private int totalElements;

  public Set<GraphNode> getNodes() {
    return nodes;
  }

  public void setNodes(Set<GraphNode> nodes) {
    this.nodes = nodes;
  }

  public Set<GraphLink> getLinks() {
    return links;
  }

  public void setLinks(Set<GraphLink> links) {
    this.links = links;
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public int getTotalElements() {
    return totalElements;
  }

  public void setTotalElements(int totalElements) {
    this.totalElements = totalElements;
  }
}
