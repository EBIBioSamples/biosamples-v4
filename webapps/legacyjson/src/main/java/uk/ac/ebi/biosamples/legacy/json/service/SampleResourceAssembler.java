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
import uk.ac.ebi.biosamples.legacy.json.domain.LegacySample;
import uk.ac.ebi.biosamples.legacy.json.domain.SamplesRelations;

@Service
public class SampleResourceAssembler
    implements ResourceAssembler<LegacySample, Resource<LegacySample>> {

  private final EntityLinks entityLinks;

  public SampleResourceAssembler(EntityLinks entityLinks) {
    this.entityLinks = entityLinks;
  }

  @Override
  public Resource<LegacySample> toResource(LegacySample entity) {

    Resource<LegacySample> resource = new Resource<>(entity);

    resource.add(
        entityLinks.linkToSingleResource(LegacySample.class, entity.accession()).withSelfRel());
    resource.add(
        entityLinks.linkToSingleResource(LegacySample.class, entity.accession()).withRel("sample"));
    resource.add(
        entityLinks
            .linkToSingleResource(SamplesRelations.class, entity.accession())
            .withRel("relations"));

    return resource;
  }
}
