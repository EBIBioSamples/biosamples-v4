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
import uk.ac.ebi.biosamples.legacy.json.domain.LegacyGroup;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacySample;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.legacy.json.service.GroupResourceAssembler;
import uk.ac.ebi.biosamples.legacy.json.service.PagedResourcesConverter;
import uk.ac.ebi.biosamples.model.Sample;

@RestController
@RequestMapping(
    value = "/groups/search",
    produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(LegacySample.class)
public class GroupsSearchController {
  private Logger log = LoggerFactory.getLogger(getClass());

  private final SampleRepository sampleRepository;
  private final GroupResourceAssembler sampleResourceAssembler;
  private final PagedResourcesConverter pagedResourcesConverter;

  public GroupsSearchController(
      SampleRepository sampleRepository,
      GroupResourceAssembler groupResourceAssembler,
      PagedResourcesConverter pagedResourcesConverter) {

    this.sampleRepository = sampleRepository;
    this.sampleResourceAssembler = groupResourceAssembler;
    this.pagedResourcesConverter = pagedResourcesConverter;
  }

  @CrossOrigin
  @GetMapping
  public Resources searchMethods() {
    log.warn("ACCESSING DEPRECATED API at GroupsSearchController /");
    Resources resources = Resources.wrap(Collections.emptyList());
    resources.add(linkTo(methodOn(this.getClass()).searchMethods()).withSelfRel());
    resources.add(
        linkTo(methodOn(this.getClass()).findByKeywords(null, null, null, null))
            .withRel("findByKeywords"));
    resources.add(
        linkTo(methodOn(this.getClass()).findByAccession(null, null, null, null))
            .withRel("findByAccession"));

    return resources;
  }

  @CrossOrigin
  @GetMapping("/findByKeywords")
  public PagedResources<Resource<LegacyGroup>> findByKeywords(
      @RequestParam(value = "keyword") String keyword,
      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
      @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
      @RequestParam(value = "sort", required = false, defaultValue = "asc") String sort) {
    log.warn("ACCESSING DEPRECATED API at GroupsSearchController /findByKeywords");
    PagedResources<Resource<Sample>> groupsByText =
        sampleRepository.findGroupsByText(keyword, page, size);
    return pagedResourcesConverter.toLegacyGroupsPagedResource(groupsByText);
  }

  @CrossOrigin
  @GetMapping("/findByAccession")
  public PagedResources<Resource<LegacyGroup>> findByAccession(
      @RequestParam(value = "accession") String accession,
      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
      @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
      @RequestParam(value = "sort", required = false, defaultValue = "asc") String sort) {
    log.warn("ACCESSING DEPRECATED API at GroupsSearchController /findByAccession");
    return findByKeywords(accession, page, size, sort);
  }
}
