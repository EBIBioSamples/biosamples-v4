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

import java.net.URI;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;

@RestController
@RequestMapping("/groups")
public class GroupsRestController {
  private static final Logger LOG = LoggerFactory.getLogger(GroupsRestController.class);

  private final SampleService sampleService;
  private final BioSamplesAapService bioSamplesAapService;
  private final SampleResourceAssembler sampleResourceAssembler;

  public GroupsRestController(
      BioSamplesAapService bioSamplesAapService,
      SampleResourceAssembler sampleResourceAssembler,
      SampleService sampleService) {
    this.bioSamplesAapService = bioSamplesAapService;
    this.sampleResourceAssembler = sampleResourceAssembler;
    this.sampleService = sampleService;
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Resource<Sample>> post(@RequestBody Sample sample) {
    if (sample.hasAccession()) {
      throw new SamplesRestController.SampleWithAccessionSumbissionException();
    }
    sample = bioSamplesAapService.handleSampleDomain(sample);

    Instant create = Instant.now();
    SubmittedViaType submittedVia =
        sample.getSubmittedVia() == null ? SubmittedViaType.JSON_API : sample.getSubmittedVia();
    sample =
        Sample.Builder.fromSample(sample)
            .withCreate(create)
            .withUpdate(create)
            .withSubmittedVia(submittedVia)
            .build();

    sample = sampleService.store(sample, true, "");
    Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample, this.getClass());
    return ResponseEntity.created(URI.create(sampleResource.getLink("self").getHref()))
        .body(sampleResource);
  }

  @PreAuthorize("isAuthenticated()")
  @PutMapping(
      path = "/{accession}",
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public Resource<Sample> put(@PathVariable String accession, @RequestBody Sample sample) {
    if (sample.getAccession() == null || !sample.getAccession().equals(accession)) {
      throw new SampleRestController.SampleAccessionMismatchException();
    }
    sample = bioSamplesAapService.handleSampleDomain(sample);

    Instant update = Instant.now();
    SubmittedViaType submittedVia =
        sample.getSubmittedVia() == null ? SubmittedViaType.JSON_API : sample.getSubmittedVia();
    sample =
        Sample.Builder.fromSample(sample).withUpdate(update).withSubmittedVia(submittedVia).build();

    sample = sampleService.store(sample, true, "");
    return sampleResourceAssembler.toResource(sample);
  }
}
