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

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;
import uk.ac.ebi.biosamples.model.JsonLDDataCatalog;
import uk.ac.ebi.biosamples.model.JsonLDDataRecord;
import uk.ac.ebi.biosamples.model.JsonLDDataset;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.JsonLDService;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.security.WebinAuthenticationService;

@RestController
@RequestMapping(produces = "application/ld+json")
public class BioschemasGetController {
  private final JsonLDService jsonLDService;
  private final SampleService sampleService;
  private final WebinAuthenticationService webinAuthenticationService;

  public BioschemasGetController(
      final JsonLDService service,
      final SampleService sampleService,
      final WebinAuthenticationService webinAuthenticationService) {
    jsonLDService = service;
    this.sampleService = sampleService;
    this.webinAuthenticationService = webinAuthenticationService;
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
  public JsonLDDataRecord getJsonLDSample(@PathVariable final String accession) {
    final Sample sample =
        sampleService
            .fetch(accession, true) // fetch returns sample with curations applied
            .orElseThrow(GlobalExceptions.SampleNotFoundException::new);
    webinAuthenticationService.isSampleAccessible(sample, null);
    return jsonLDService.sampleToJsonLD(sample);
  }
}
