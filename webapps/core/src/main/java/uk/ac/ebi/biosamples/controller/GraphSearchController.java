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
package uk.ac.ebi.biosamples.controller;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo4j.model.CypherQuery;
import uk.ac.ebi.biosamples.neo4j.model.GraphSearchQuery;
import uk.ac.ebi.biosamples.service.GraphSearchService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;

@RestController
@RequestMapping("/graph/search")
public class GraphSearchController {
  private static final Logger LOG = LoggerFactory.getLogger(GraphSearchController.class);

  private final SampleResourceAssembler sampleResourceAssembler;
  private final GraphSearchService graphSearchService;

  public GraphSearchController(
      SampleResourceAssembler sampleResourceAssembler, GraphSearchService graphSearchService) {
    this.sampleResourceAssembler = sampleResourceAssembler;
    this.graphSearchService = graphSearchService;
  }

  @PostMapping(
      path = "/cypher",
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public CypherQuery executeCypher(@RequestBody CypherQuery cypherQuery) {
    CypherQuery cypherQueryResponse = new CypherQuery();
    cypherQueryResponse.setQuery(cypherQuery.getQuery());
    cypherQueryResponse.setResponse(graphSearchService.executeCypher(cypherQuery.getQuery()));

    return cypherQueryResponse;
  }

  @PostMapping(
      path = "",
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public GraphSearchQuery graphSearch(@RequestBody GraphSearchQuery query) {
    int effectiveSize;
    int effectivePage;
    int totalElements = 0;
    int totalPages = 0;

    if (query.getSize() > 100) {
      effectiveSize = 100;
    } else if (query.getSize() < 1) {
      effectiveSize = 10;
    } else {
      effectiveSize = query.getSize();
    }
    effectivePage = Math.max(0, query.getPage());

    return graphSearchService.graphSearch(query, effectiveSize, effectivePage);
  }

  private Resources<Resource<Sample>> populateResources(
      List<Sample> samples,
      int effectiveSize,
      int effectivePage,
      int totalElements,
      int totalPages) {
    PagedResources.PageMetadata pageMetadata =
        new PagedResources.PageMetadata(effectiveSize, effectivePage, totalElements, totalPages);

    Resources<Resource<Sample>> resources =
        new PagedResources<>(
            samples.stream()
                .map(
                    s ->
                        s != null
                            ? sampleResourceAssembler.toResource(s, SampleRestController.class)
                            : null)
                .collect(Collectors.toList()),
            pageMetadata);

    return resources;
  }
}
