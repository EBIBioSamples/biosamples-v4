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
package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.controller.SampleCurationLinksRestController;
import uk.ac.ebi.biosamples.controller.SampleRestController;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;

@Service
public class CurationLinkResourceAssembler
    implements ResourceAssembler<CurationLink, Resource<CurationLink>> {

  private final EntityLinks entityLinks;

  public CurationLinkResourceAssembler(EntityLinks entityLinks) {
    this.entityLinks = entityLinks;
  }

  @Override
  public Resource<CurationLink> toResource(CurationLink curationLink) {
    Resource<CurationLink> resource = new Resource<>(curationLink);

    resource.add(
        ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(SampleCurationLinksRestController.class)
                    .getCurationLinkJson(curationLink.getSample(), curationLink.getHash()))
            .withSelfRel());

    resource.add(
        ControllerLinkBuilder.linkTo(SampleRestController.class, curationLink.getSample())
            .withRel("sample"));

    resource.add(
        entityLinks
            .linkToSingleResource(Curation.class, curationLink.getCuration().getHash())
            .withRel("curation"));

    return resource;
  }
}
