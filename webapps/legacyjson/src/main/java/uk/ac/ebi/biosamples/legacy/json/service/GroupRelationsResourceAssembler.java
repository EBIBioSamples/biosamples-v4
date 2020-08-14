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
package uk.ac.ebi.biosamples.legacy.json.service;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.legacy.json.controller.GroupsRelationsController;
import uk.ac.ebi.biosamples.legacy.json.domain.GroupsRelations;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacyGroup;

@Service
public class GroupRelationsResourceAssembler
    implements ResourceAssembler<GroupsRelations, Resource<GroupsRelations>> {

  private EntityLinks entityLinks;

  public GroupRelationsResourceAssembler(EntityLinks entityLinks) {
    this.entityLinks = entityLinks;
  }

  @Override
  public Resource<GroupsRelations> toResource(GroupsRelations entity) {

    Resource<GroupsRelations> resource = new Resource<>(entity);

    resource.add(
        entityLinks.linkToSingleResource(GroupsRelations.class, entity.accession()).withSelfRel());
    resource.add(
        entityLinks.linkToSingleResource(LegacyGroup.class, entity.accession()).withRel("details"));
    resource.add(
        entityLinks
            .linkToSingleResource(GroupsRelations.class, entity.accession())
            .withRel("groupsrelations"));
    resource.add(new Link("test").withRel("externallinks"));
    //
    // resource.add(entityLinks.linkToCollectionResource(LegacyExternalReference.class).withRel("externallinks"));
    resource.add(
        linkTo(
                methodOn(GroupsRelationsController.class)
                    .getGroupSamplesRelations(entity.accession()))
            .withRel("samples"));

    return resource;
  }
}
