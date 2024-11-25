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
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.Accession;
import uk.ac.ebi.biosamples.service.AccessionsService;
import uk.ac.ebi.biosamples.service.SampleService;

@RestController
@RequestMapping("/accessions")
@CrossOrigin
public class AccessionsGetController {
  private final AccessionsService accessionsService;
  private final SampleService sampleService;

  public AccessionsGetController(
      final AccessionsService accessionsService, final SampleService sampleService) {
    this.accessionsService = accessionsService;
    this.sampleService = sampleService;
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<CollectionModel<Accession>> getAccessions(
      @RequestParam(name = "text", required = false) final String text,
      @RequestParam(name = "filter", required = false) final String[] filter,
      @RequestParam(name = "page", required = false, defaultValue = "0") final Integer page,
      @RequestParam(name = "size", required = false, defaultValue = "100") final Integer size) {
    final Authentication loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final String principle = sampleService.getPrinciple(loggedInUser);

    final Page<String> pageAccessions =
        accessionsService.getAccessions(text, filter, principle, page, size);
    final PagedModel.PageMetadata pageMetadata =
        new PagedModel.PageMetadata(
            size,
            pageAccessions.getNumber(),
            pageAccessions.getTotalElements(),
            pageAccessions.getTotalPages());
    final CollectionModel<Accession> resources =
        PagedModel.of(
            pageAccessions.getContent().stream().map(Accession::build).collect(Collectors.toList()),
            pageMetadata);

    addRelLinks(pageAccessions, resources, text, filter, page, size);

    return ResponseEntity.ok().body(resources);
  }

  private void addRelLinks(
      final Page<String> pageAccessions,
      final CollectionModel<Accession> resources,
      final String text,
      final String[] filter,
      final Integer page,
      final Integer size) {
    resources.add(
        SamplesRestController.getPageLink(
            text, filter, page, size, null, IanaLinkRelations.SELF.value(), getClass()));

    if (pageAccessions.getTotalPages() > 1) {
      resources.add(
          SamplesRestController.getPageLink(
              text, filter, 0, size, null, IanaLinkRelations.FIRST.value(), getClass()));
      resources.add(
          SamplesRestController.getPageLink(
              text,
              filter,
              pageAccessions.getTotalPages(),
              size,
              null,
              IanaLinkRelations.LAST.value(),
              getClass()));
    }

    if (page > 0) {
      resources.add(
          SamplesRestController.getPageLink(
              text, filter, page - 1, size, null, IanaLinkRelations.PREVIOUS.value(), getClass()));
    }

    if (page < pageAccessions.getTotalPages() - 1) {
      resources.add(
          SamplesRestController.getPageLink(
              text, filter, page + 1, size, null, IanaLinkRelations.NEXT.value(), getClass()));
    }
  }
}
