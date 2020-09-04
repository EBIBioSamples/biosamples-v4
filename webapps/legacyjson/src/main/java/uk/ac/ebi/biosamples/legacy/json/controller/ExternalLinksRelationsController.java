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
package uk.ac.ebi.biosamples.legacy.json.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.legacy.json.domain.ExternalLinksRelation;
import uk.ac.ebi.biosamples.legacy.json.repository.RelationsRepository;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.legacy.json.service.ExternalLinksResourceAssembler;
import uk.ac.ebi.biosamples.legacy.json.service.PagedResourcesConverter;

@RestController
@RequestMapping(
    value = "/externallinksrelations",
    produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(ExternalLinksRelation.class)
public class ExternalLinksRelationsController {
  private Logger log = LoggerFactory.getLogger(getClass());

  private final EntityLinks entityLinks;
  private final SampleRepository sampleRepository;
  private final RelationsRepository relationsRepository;
  private final PagedResourcesConverter pagedResourcesConverter;
  private final ExternalLinksResourceAssembler externalLinksResourceAssembler;

  public ExternalLinksRelationsController(
      EntityLinks entityLinks,
      SampleRepository sampleRepository,
      RelationsRepository relationsRepository,
      PagedResourcesConverter pagedResourcesConverter,
      ExternalLinksResourceAssembler externalLinksResourceAssembler) {

    this.entityLinks = entityLinks;
    this.sampleRepository = sampleRepository;
    this.relationsRepository = relationsRepository;
    this.pagedResourcesConverter = pagedResourcesConverter;
    this.externalLinksResourceAssembler = externalLinksResourceAssembler;
  }

  @CrossOrigin
  @GetMapping
  public PagedResources<Resource<ExternalLinksRelation>> allSamplesRelations(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "50") int size) {
    log.warn("ACCESSING DEPRECATED API at ExternalLinksRelationsController /");

    PagedResources pagedResources =
        pagedResourcesConverter.toExternalLinksRelationPagedResource(null);
    pagedResources.add(
        linkTo(methodOn(ExternalLinksRelationsController.class).searchMethods()).withRel("search"));
    return pagedResources;
  }

  public Resources searchMethods() {
    Resources resources = Resources.wrap(Collections.emptyList());
    resources.add(linkTo(methodOn(this.getClass()).searchMethods()).withSelfRel());

    return resources;
  }
}
