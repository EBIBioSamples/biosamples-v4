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

import java.util.concurrent.TimeUnit;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class RootHalController {

  public RootHalController() {}

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(produces = {MediaTypes.HAL_JSON_VALUE})
  public ResponseEntity<ResourceSupport> rootHal() {
    ResourceSupport resource = new ResourceSupport();

    resource.add(ControllerLinkBuilder.linkTo(SamplesRestController.class).withRel("samples"));
    resource.add(ControllerLinkBuilder.linkTo(GroupsRestController.class).withRel("groups"));
    resource.add(ControllerLinkBuilder.linkTo(CurationRestController.class).withRel("curations"));
    resource.add(
        new Link(
            "https://www.ebi.ac.uk/data-protection/privacy-notice/embl-ebi-public-website",
            "privacyNotice"));
    resource.add(new Link("https://www.ebi.ac.uk/about/terms-of-use", "termsOfUse"));

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CACHE_CONTROL,
            CacheControl.maxAge(60, TimeUnit.MINUTES).cachePublic().getHeaderValue())
        .body(resource);
  }
}
