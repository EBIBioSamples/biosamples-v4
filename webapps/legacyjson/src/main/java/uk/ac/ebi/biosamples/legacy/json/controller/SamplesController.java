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

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    value = "/samples",
    produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
@ExposesResourceFor(LegacySample.class)
public class SamplesController {
  Logger log = LoggerFactory.getLogger(getClass());

  private final SampleRepository sampleRepository;
  private final PagedResourcesConverter pagedResourcesConverter;
  private final SampleResourceAssembler sampleResourceAssembler;

  @Autowired
  public SamplesController(
      SampleRepository sampleRepository,
      PagedResourcesConverter pagedResourcesConverter,
      SampleResourceAssembler sampleResourceAssembler) {

    this.sampleRepository = sampleRepository;
    this.pagedResourcesConverter = pagedResourcesConverter;
    this.sampleResourceAssembler = sampleResourceAssembler;
  }

  @CrossOrigin
  @GetMapping(value = "/{accession:SAM[END]A?\\d+}")
  public ResponseEntity<Resource<LegacySample>> sampleByAccession(@PathVariable String accession) {
    log.warn("ACCESSING DEPRECATED API at SamplesController /{accession:SAM[END]A?\\d+}");

    Optional<Sample> sample = sampleRepository.findByAccession(accession);
    if (!sample.isPresent()) {
      return ResponseEntity.notFound().build();
    }

    LegacySample v3TestSample = new LegacySample(sample.get());
    return ResponseEntity.ok(sampleResourceAssembler.toResource(v3TestSample));
  }

  @CrossOrigin
  @GetMapping
  public PagedResources<Resource<LegacySample>> allSamples(
      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
      @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
      @RequestParam(value = "sort", required = false, defaultValue = "asc") String sort) {
    log.warn("ACCESSING DEPRECATED API at SamplesController /");

    PagedResources<Resource<Sample>> samples = sampleRepository.findSamples(page, size);
    PagedResources<Resource<LegacySample>> pagedResources =
        pagedResourcesConverter.toLegacySamplesPagedResource(samples);
    pagedResources.add(
        linkTo(methodOn(SamplesSearchController.class).searchMethods()).withRel("search"));

    return pagedResources;
  }
}
