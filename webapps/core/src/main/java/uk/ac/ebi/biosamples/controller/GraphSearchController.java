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
package uk.ac.ebi.biosamples.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.neo4j.model.CypherQuery;
import uk.ac.ebi.biosamples.neo4j.model.GraphSearchQuery;
import uk.ac.ebi.biosamples.service.GraphSearchService;

@RestController
@RequestMapping("/graph/search")
public class GraphSearchController {
  private final GraphSearchService graphSearchService;

  public GraphSearchController(final GraphSearchService graphSearchService) {
    this.graphSearchService = graphSearchService;
  }

  @PostMapping(
      path = "/cypher",
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public CypherQuery executeCypher(@RequestBody final CypherQuery cypherQuery) {
    final CypherQuery cypherQueryResponse = new CypherQuery();
    cypherQueryResponse.setQuery(cypherQuery.getQuery());
    cypherQueryResponse.setResponse(graphSearchService.executeCypher(cypherQuery.getQuery()));

    return cypherQueryResponse;
  }

  @PostMapping(
      path = "",
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public GraphSearchQuery graphSearch(@RequestBody final GraphSearchQuery query) {
    final int effectiveSize;
    final int effectivePage;

    if (query.getSize() > 100) {
      effectiveSize = 100;
    } else if (query.getSize() < 1) {
      effectiveSize = 10;
    } else {
      effectiveSize = query.getSize();
    }
    effectivePage = Math.max(1, query.getPage());

    return graphSearchService.graphSearch(query, effectiveSize, effectivePage);
  }
}
