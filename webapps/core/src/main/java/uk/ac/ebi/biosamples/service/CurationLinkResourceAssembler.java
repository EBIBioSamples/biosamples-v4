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
package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.controller.SampleCurationLinksController;
import uk.ac.ebi.biosamples.controller.SampleRestController;
import uk.ac.ebi.biosamples.core.model.Curation;
import uk.ac.ebi.biosamples.core.model.CurationLink;

@Service
public class CurationLinkResourceAssembler
    implements RepresentationModelAssembler<CurationLink, EntityModel<CurationLink>> {
  private final EntityLinks entityLinks;

  public CurationLinkResourceAssembler(EntityLinks entityLinks) {
    this.entityLinks = entityLinks;
  }

  @Override
  public EntityModel<CurationLink> toModel(CurationLink curationLink) {
    EntityModel<CurationLink> resource = EntityModel.of(curationLink);

    resource.add(
        WebMvcLinkBuilder.linkTo(
                WebMvcLinkBuilder.methodOn(SampleCurationLinksController.class)
                    .getCurationLinkJson(curationLink.getSample(), curationLink.getHash()))
            .withSelfRel());

    resource.add(
        WebMvcLinkBuilder.linkTo(SampleRestController.class, curationLink.getSample())
            .withRel("sample"));

    resource.add(
        entityLinks
            .linkToItemResource(Curation.class, curationLink.getCuration().getHash())
            .withRel("curation"));

    return resource;
  }

  @Override
  public CollectionModel<EntityModel<CurationLink>> toCollectionModel(
      Iterable<? extends CurationLink> entities) {
    return null;
  }
}
