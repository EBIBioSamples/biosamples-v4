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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacySample;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.legacy.json.service.PagedResourcesConverter;
import uk.ac.ebi.biosamples.legacy.json.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.model.Sample;

@RestController
@RequestMapping(
    value = "/samples/search",
    produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(LegacySample.class)
public class SamplesSearchController {
  Logger log = LoggerFactory.getLogger(getClass());

  private final SampleRepository sampleRepository;
  private final SampleResourceAssembler sampleResourceAssembler;
  private final PagedResourcesConverter pagedResourcesConverter;

  public SamplesSearchController(
      SampleResourceAssembler sampleResourceAssembler,
      SampleRepository sampleRepository,
      PagedResourcesConverter pagedResourcesConverter) {

    this.sampleRepository = sampleRepository;
    this.sampleResourceAssembler = sampleResourceAssembler;
    this.pagedResourcesConverter = pagedResourcesConverter;
  }

  @CrossOrigin
  @GetMapping
  public Resources searchMethods() {
    log.warn("ACCESSING DEPRECATED API at SamplesSearchController /");
    Resources resources = Resources.wrap(Collections.emptyList());
    resources.add(linkTo(methodOn(this.getClass()).searchMethods()).withSelfRel());
    resources.add(
        linkTo(methodOn(this.getClass()).findFirstSampleContainedInAGroup(null))
            .withRel("findFirstByGroupsContains"));
    resources.add(
        linkTo(methodOn(this.getClass()).findByGroups(null, null, null, null))
            .withRel("findByGroups"));
    resources.add(
        linkTo(methodOn(this.getClass()).findByAccession(null, null, null, null))
            .withRel("findByAccession"));
    resources.add(
        linkTo(methodOn(this.getClass()).findByText(null, null, null, null)).withRel("findByText"));
    resources.add(
        linkTo(methodOn(this.getClass()).findByTextAndGroups(null, null, null, null, null))
            .withRel("findByTextAndGroups"));
    resources.add(
        linkTo(methodOn(this.getClass()).findByAccessionAndGroups(null, null, null, null, null))
            .withRel("findByAccessionAndGroups"));

    return resources;
  }

  @CrossOrigin
  @GetMapping("/findFirstByGroupsContains")
  public ResponseEntity<Resource<LegacySample>> findFirstSampleContainedInAGroup(
      @RequestParam(value = "group", required = false, defaultValue = "") String group) {
    log.warn("ACCESSING DEPRECATED API at SamplesSearchController /findFirstByGroupsContains");
    Optional<Resource<Sample>> optionalSampleResource =
        sampleRepository.findFirstSampleByGroup(group);

    if (!optionalSampleResource.isPresent()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(
        sampleResourceAssembler.toResource(
            new LegacySample(optionalSampleResource.get().getContent())));
  }

  @CrossOrigin
  @GetMapping("/findByGroups")
  public PagedResources<Resource<LegacySample>> findByGroups(
      @RequestParam(value = "group", required = false, defaultValue = "") String groupAccession,
      @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
      @RequestParam(value = "sort", required = false, defaultValue = "asc") String sort) {
    log.warn("ACCESSING DEPRECATED API at SamplesSearchController /findByGroups");

    PagedResources<Resource<Sample>> samplePagedResources =
        sampleRepository.findSamplesByGroup(groupAccession, page, size);
    return pagedResourcesConverter.toLegacySamplesPagedResource(samplePagedResources);
  }

  @CrossOrigin
  @GetMapping("/findByAccession")
  public PagedResources findByAccession(
      @RequestParam(value = "accession", required = false, defaultValue = "") String accession,
      @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
      @RequestParam(value = "sort", required = false, defaultValue = "asc") String sort) {
    log.warn("ACCESSING DEPRECATED API at SamplesSearchController /findByAccession");
    PagedResources<Resource<Sample>> samplesPagedResourcesByAccession =
        sampleRepository.findSamplesByText(accession, page, size);
    return pagedResourcesConverter.toLegacySamplesPagedResource(samplesPagedResourcesByAccession);
  }

  @CrossOrigin
  @GetMapping("/findByText")
  public PagedResources<Resource<LegacySample>> findByText(
      @RequestParam(value = "text", required = false, defaultValue = "*:*") String text,
      @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
      @RequestParam(value = "sort", defaultValue = "asc") String sort) {
    log.warn("ACCESSING DEPRECATED API at SamplesSearchController /findByText");
    PagedResources<Resource<Sample>> samplesPagedResourcesByText =
        sampleRepository.findSamplesByText(text, page, size);
    return pagedResourcesConverter.toLegacySamplesPagedResource(samplesPagedResourcesByText);
  }

  @CrossOrigin
  @GetMapping("/findByTextAndGroups")
  public PagedResources<Resource<LegacySample>> findByTextAndGroups(
      @RequestParam(value = "text", required = false, defaultValue = "") String text,
      @RequestParam(value = "group", required = false, defaultValue = "") String groupAccession,
      @RequestParam(value = "size", defaultValue = "50") Integer size,
      @RequestParam(value = "page", defaultValue = "0") Integer page,
      @RequestParam(value = "sort", defaultValue = "asc") String sort) {
    log.warn("ACCESSING DEPRECATED API at SamplesSearchController /findByTextAndGroups");
    PagedResources<Resource<Sample>> samples =
        sampleRepository.findSamplesByTextAndGroup(text, groupAccession, page, size);
    return pagedResourcesConverter.toLegacySamplesPagedResource(samples);
  }

  @CrossOrigin
  @GetMapping("/findByAccessionAndGroups")
  public PagedResources<Resource<LegacySample>> findByAccessionAndGroups(
      @RequestParam(value = "accession", required = false, defaultValue = "") String accession,
      @RequestParam(value = "group", required = false, defaultValue = "") String groupAccession,
      @RequestParam(value = "size", defaultValue = "50") Integer size,
      @RequestParam(value = "page", defaultValue = "0") Integer page,
      @RequestParam(value = "sort", defaultValue = "asc") String sort) {
    log.warn("ACCESSING DEPRECATED API at SamplesSearchController /findByAccessionAndGroups");
    PagedResources<Resource<Sample>> sample =
        sampleRepository.findSampleInGroup(accession, groupAccession);
    return pagedResourcesConverter.toLegacySamplesPagedResource(sample);
  }
}
