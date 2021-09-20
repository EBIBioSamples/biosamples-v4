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

import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.Accession;
import uk.ac.ebi.biosamples.service.AccessionsService;

@RestController
@RequestMapping("/accessions")
@CrossOrigin
public class AccessionsRestController {

  private final AccessionsService accessionsService;

  public AccessionsRestController(AccessionsService accessionsService) {
    this.accessionsService = accessionsService;
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Resources<Accession>> getAccessions(
      @RequestParam(name = "text", required = false) String text,
      @RequestParam(name = "filter", required = false) String[] filter,
      @RequestParam(name = "page", required = false) final Integer page,
      @RequestParam(name = "size", required = false) final Integer size) {

    int effectiveSize = size == null ? 100 : size;
    int effectivePage = page == null ? 0 : page;

    Page<String> pageAccessions =
        accessionsService.getAccessions(text, filter, effectivePage, effectiveSize);

    PagedResources.PageMetadata pageMetadata =
        new PagedResources.PageMetadata(
            effectiveSize,
            pageAccessions.getNumber(),
            pageAccessions.getTotalElements(),
            pageAccessions.getTotalPages());

    Resources<Accession> resources =
        new PagedResources<>(
            pageAccessions.getContent().stream().map(Accession::build).collect(Collectors.toList()),
            pageMetadata);
    addRelLinks(pageAccessions, resources, text, filter, effectivePage, effectiveSize);

    return ResponseEntity.ok().body(resources);
  }

  private void addRelLinks(
      Page<String> pageAccessions,
      Resources<Accession> resources,
      String text,
      String[] filter,
      Integer effectivePage,
      Integer effectiveSize) {
    resources.add(
        SamplesRestController.getPageLink(
            text, filter, effectivePage, effectiveSize, null, Link.REL_SELF, this.getClass()));

    // if theres more than one page, link to first and last
    if (pageAccessions.getTotalPages() > 1) {
      resources.add(
          SamplesRestController.getPageLink(
              text, filter, 0, effectiveSize, null, Link.REL_FIRST, this.getClass()));
      resources.add(
          SamplesRestController.getPageLink(
              text,
              filter,
              pageAccessions.getTotalPages(),
              effectiveSize,
              null,
              Link.REL_LAST,
              this.getClass()));
    }
    // if there was a previous page, link to it
    if (effectivePage > 0) {
      resources.add(
          SamplesRestController.getPageLink(
              text,
              filter,
              effectivePage - 1,
              effectiveSize,
              null,
              Link.REL_PREVIOUS,
              this.getClass()));
    }

    // if there is a next page, link to it
    if (effectivePage < pageAccessions.getTotalPages() - 1) {
      resources.add(
          SamplesRestController.getPageLink(
              text,
              filter,
              effectivePage + 1,
              effectiveSize,
              null,
              Link.REL_NEXT,
              this.getClass()));
    }
  }
}
