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

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.legacy.json.domain.GroupsRelations;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacyGroup;

@Service
public class GroupResourceAssembler
    implements ResourceAssembler<LegacyGroup, Resource<LegacyGroup>> {

  private final EntityLinks entityLinks;

  public GroupResourceAssembler(EntityLinks entityLinks) {
    this.entityLinks = entityLinks;
  }

  @Override
  public Resource<LegacyGroup> toResource(LegacyGroup entity) {

    Resource<LegacyGroup> resource = new Resource<>(entity);

    resource.add(
        entityLinks.linkToSingleResource(LegacyGroup.class, entity.accession()).withSelfRel());
    resource.add(
        entityLinks.linkToSingleResource(LegacyGroup.class, entity.accession()).withRel("group"));
    resource.add(
        entityLinks
            .linkToSingleResource(GroupsRelations.class, entity.accession())
            .withRel("relations"));

    return resource;
  }
}
