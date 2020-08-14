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
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

  Logger log = LoggerFactory.getLogger(getClass());

  @CrossOrigin
  @GetMapping(
      value = "/",
      produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public Resources root() {
    log.warn("ACCESSING DEPRECATED API at IndexController /");

    Resources resources = Resources.wrap(Collections.emptyList());
    resources.add(
        linkTo(methodOn(SamplesController.class).allSamples(null, null, null)).withRel("samples"));
    resources.add(
        linkTo(methodOn(GroupsController.class).allGroups(null, null, null)).withRel("groups"));
    resources.add(
        linkTo(methodOn(SamplesRelationsController.class).allSamplesRelations(null, null, null))
            .withRel("samplesrelations"));
    resources.add(
        linkTo(methodOn(GroupsRelationsController.class).allGroupsRelations(null, null, null))
            .withRel("groupsrelations"));
    //
    // resources.add(linkTo(methodOn(ExternalLinksRelationsController.class).allSamplesRelations(null, null).withRel("externallinksrelations"));

    return resources;
  }
}
