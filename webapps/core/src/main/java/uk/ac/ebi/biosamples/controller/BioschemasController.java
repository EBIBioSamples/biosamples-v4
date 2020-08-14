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
package uk.ac.ebi.biosamples.controller;

import java.time.Instant;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exception.SampleNotAccessibleException;
import uk.ac.ebi.biosamples.exception.SampleNotFoundException;
import uk.ac.ebi.biosamples.model.JsonLDDataCatalog;
import uk.ac.ebi.biosamples.model.JsonLDDataRecord;
import uk.ac.ebi.biosamples.model.JsonLDDataset;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.JsonLDService;
import uk.ac.ebi.biosamples.service.SampleService;

@RestController
@RequestMapping(produces = "application/ld+json")
public class BioschemasController {

  private final JsonLDService jsonLDService;
  private final SampleService sampleService;
  private final BioSamplesAapService bioSamplesAapService;

  public BioschemasController(
      JsonLDService service,
      SampleService sampleService,
      BioSamplesAapService bioSamplesAapService) {
    this.jsonLDService = service;
    this.sampleService = sampleService;
    this.bioSamplesAapService = bioSamplesAapService;
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(value = "/")
  public JsonLDDataCatalog rootBioschemas() {
    return jsonLDService.getBioSamplesDataCatalog();
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(value = "/samples")
  public JsonLDDataset biosamplesDataset() {
    return jsonLDService.getBioSamplesDataset();
  }

  @PreAuthorize("isAuthenticated()")
  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(value = "/samples/{accession}", produces = "application/ld+json")
  public JsonLDDataRecord getJsonLDSample(
      @PathVariable String accession,
      @RequestParam(name = "curationrepo", required = false) final String curationRepo) {
    Optional<Sample> sample = sampleService.fetch(accession, Optional.empty(), curationRepo);
    if (!sample.isPresent()) {
      throw new SampleNotFoundException();
    }
    bioSamplesAapService.checkAccessible(sample.get());

    // check if the release date is in the future and if so return it as
    // private
    if (sample.get().getRelease().isAfter(Instant.now())) {
      throw new SampleNotAccessibleException();
    }

    return jsonLDService.sampleToJsonLD(sample.get());
  }
}
