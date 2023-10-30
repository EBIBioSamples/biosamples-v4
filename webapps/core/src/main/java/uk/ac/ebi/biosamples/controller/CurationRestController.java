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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.service.CurationReadService;
import uk.ac.ebi.biosamples.service.CurationResourceAssembler;
import uk.ac.ebi.biosamples.service.SamplePageService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;

@RestController
@ExposesResourceFor(Curation.class)
@RequestMapping("/curations")
public class CurationRestController {
  private final SamplePageService samplePageService;
  private final CurationReadService curationReadService;
  private final SampleResourceAssembler sampleResourceAssembler;
  private final CurationResourceAssembler curationResourceAssembler;
  private final EntityLinks entityLinks;

  public CurationRestController(
      final SamplePageService samplePageService,
      final CurationReadService curationService,
      final SampleResourceAssembler sampleResourceAssembler,
      final CurationResourceAssembler curationResourceAssembler,
      final EntityLinks entityLinks) {
    this.samplePageService = samplePageService;
    this.entityLinks = entityLinks;
    this.sampleResourceAssembler = sampleResourceAssembler;
    curationReadService = curationService;
    this.curationResourceAssembler = curationResourceAssembler;
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<PagedModel<EntityModel<Curation>>> getPagedHal(
      final Pageable page, final PagedResourcesAssembler<Curation> pageAssembler) {
    final Page<Curation> pageExternalReference = curationReadService.getPage(page);
    final PagedModel<EntityModel<Curation>> pagedResources =
        pageAssembler.toModel(
            pageExternalReference,
            curationResourceAssembler,
            entityLinks.linkToCollectionResource(Curation.class));

    return ResponseEntity.ok().body(pagedResources);
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(
      value = "/{hash}",
      produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<EntityModel<Curation>> getCurationHal(@PathVariable final String hash) {
    final Curation curation = curationReadService.getCuration(hash);
    if (curation == null) {
      return ResponseEntity.notFound().build();
    }
    final EntityModel<Curation> resource = curationResourceAssembler.toModel(curation);
    return ResponseEntity.ok().body(resource);
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(
      value = "/{hash}/samples",
      produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<PagedModel<EntityModel<Sample>>> getCurationSamplesHal(
      @PathVariable final String hash,
      final Pageable pageable,
      final PagedResourcesAssembler<Sample> pageAssembler) {

    // get the response as if we'd called the externalReference endpoint
    final ResponseEntity<EntityModel<Curation>> externalReferenceResponse = getCurationHal(hash);
    if (!externalReferenceResponse.getStatusCode().is2xxSuccessful()) {
      // propagate any non-2xx status code from /{id}/ to this endpoint
      return ResponseEntity.status(externalReferenceResponse.getStatusCode()).build();
    }

    // get the content from the services
    final Page<Sample> pageSample = samplePageService.getSamplesOfCuration(hash, pageable);

    // use the resource assembler and a link to this method to build out the response content
    final PagedModel<EntityModel<Sample>> pagedResources =
        pageAssembler.toModel(
            pageSample,
            sampleResourceAssembler,
            WebMvcLinkBuilder.linkTo(
                    WebMvcLinkBuilder.methodOn(CurationRestController.class)
                        .getCurationSamplesHal(hash, pageable, pageAssembler))
                .withRel("samples"));

    return ResponseEntity.ok().body(pagedResources);
  }
}
