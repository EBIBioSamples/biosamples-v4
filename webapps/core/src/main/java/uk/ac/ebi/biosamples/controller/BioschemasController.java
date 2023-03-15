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

import java.time.Instant;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.JsonLDDataCatalog;
import uk.ac.ebi.biosamples.model.JsonLDDataRecord;
import uk.ac.ebi.biosamples.model.JsonLDDataset;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.JsonLDService;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;

@RestController
@RequestMapping(produces = "application/ld+json")
public class BioschemasController {
  private final JsonLDService jsonLDService;
  private final SampleService sampleService;
  private final BioSamplesAapService bioSamplesAapService;

  public BioschemasController(
      final JsonLDService service,
      final SampleService sampleService,
      final BioSamplesAapService bioSamplesAapService) {
    jsonLDService = service;
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
  public JsonLDDataRecord getJsonLDSample(@PathVariable String accession) {
    Sample sample = sampleService.fetch(accession, Optional.empty())
        .orElseThrow(() -> new GlobalExceptions.SampleNotFoundException());
    bioSamplesAapService.isSampleAccessible(sample);
    return jsonLDService.sampleToJsonLD(sample);
  }
}
