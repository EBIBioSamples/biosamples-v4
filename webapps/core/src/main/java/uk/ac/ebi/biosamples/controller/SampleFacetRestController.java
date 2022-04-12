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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FacetService;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.utils.LinkUtils;

@RestController
@ExposesResourceFor(Facet.class)
@RequestMapping("/samples/facets")
public class SampleFacetRestController {

  private final FacetService facetService;
  private final FilterService filterService;

  private final EntityLinks entityLinks;

  private Logger log = LoggerFactory.getLogger(getClass());

  public SampleFacetRestController(
      FacetService facetService, FilterService filterService, EntityLinks entityLinks) {
    this.facetService = facetService;
    this.filterService = filterService;
    this.entityLinks = entityLinks;
  }

  @CrossOrigin
  @GetMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Resources<Facet>> getFacetsHal(
      @RequestParam(name = "text", required = false) String text,
      @RequestParam(name = "filter", required = false) String[] filter) {

    // TODO support rows and start parameters
    //		MultiValueMap<String, String> filters = filterService.getFilters(filter);
    Collection<Filter> filters = filterService.getFiltersCollection(filter);
    Collection<String> domains = Collections.emptyList();
    List<Facet> sampleFacets = facetService.getFacets(text, filters, domains, 10, 10);

    //    	PagedResources<StringListFacet> resources = new PagedResources<>(
    //    			sampleFacets,
    //				new PagedResources.PageMetadata(10, 1, 10, 5));
    Resources<Facet> resources = new Resources<>(sampleFacets);

    // Links for the entire page
    // this is hacky, but no clear way to do this in spring-hateoas currently
    resources.removeLinks();

    // to generate the HAL template correctly, the parameter name must match the requestparam
    // name
    resources.add(
        LinkUtils.cleanLink(
            ControllerLinkBuilder.linkTo(
                    ControllerLinkBuilder.methodOn(SampleFacetRestController.class)
                        .getFacetsHal(text, filter))
                .withSelfRel()));

    resources.add(
        LinkUtils.cleanLink(
            ControllerLinkBuilder.linkTo(
                    ControllerLinkBuilder.methodOn(SamplesRestController.class)
                        .searchHal(text, filter, null, null, null, null, null, null, "AAP"))
                .withRel("samples")));

    return ResponseEntity.ok().body(resources);
  }
}
