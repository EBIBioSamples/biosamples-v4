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
import uk.ac.ebi.biosamples.legacy.json.domain.ExternalLinksRelation;

@Service
public class ExternalLinksResourceAssembler
    implements ResourceAssembler<ExternalLinksRelation, Resource<ExternalLinksRelation>> {

  private final EntityLinks entityLinks;

  public ExternalLinksResourceAssembler(EntityLinks entityLinks) {
    this.entityLinks = entityLinks;
  }

  @Override
  public Resource<ExternalLinksRelation> toResource(ExternalLinksRelation entity) {
    Resource resource = new Resource(entity);
    resource.add(
        entityLinks.linkToSingleResource(ExternalLinksRelation.class, entity.url()).withSelfRel());
    resource.add(
        entityLinks
            .linkToSingleResource(ExternalLinksRelation.class, entity.url())
            .withRel("externallinkrelations"));
    //        resource.add(new Link("test").withRel("samples"));
    //        resource.add(new Link("test").withRel("groups"));
    return resource;
  }
}
