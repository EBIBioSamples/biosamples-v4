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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FacetService;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.utils.LinkUtils;

@RestController
@ExposesResourceFor(Facet.class)
@RequestMapping("/samples/facets")
public class SampleFacetController {
  private final FacetService facetService;
  private final FilterService filterService;

  public SampleFacetController(final FacetService facetService, final FilterService filterService) {
    this.facetService = facetService;
    this.filterService = filterService;
  }

  @CrossOrigin
  @GetMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<CollectionModel<Facet>> getFacetsHal(
      @RequestParam(name = "text", required = false) final String text,
      @RequestParam(name = "filter", required = false) final String[] filter) {

    // TODO support rows and start parameters
    //		MultiValueMap<String, String> filters = filterService.getFilters(filter);
    final Collection<Filter> filters = filterService.getFiltersCollection(filter);
    final Collection<String> domains = Collections.emptyList();
    final List<Facet> sampleFacets = facetService.getFacets(text, filters, 10, 10);

    final CollectionModel<Facet> resources = CollectionModel.of(sampleFacets);

    // Links for the entire page
    // this is hacky, but no clear way to do this in spring-hateoas currently
    resources.removeLinks();

    // to generate the HAL template correctly, the parameter name must match the requestparam
    // name
    resources.add(
        LinkUtils.cleanLink(
            WebMvcLinkBuilder.linkTo(
                    WebMvcLinkBuilder.methodOn(SampleFacetController.class)
                        .getFacetsHal(text, filter))
                .withSelfRel()));

    resources.add(
        LinkUtils.cleanLink(
            WebMvcLinkBuilder.linkTo(
                    WebMvcLinkBuilder.methodOn(SamplesRestController.class)
                        .searchHal(text, filter, null, null, null, null, true))
                .withRel("samples")));

    return ResponseEntity.ok().body(resources);
  }
}
