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
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.legacy.json.controller.SamplesController;
import uk.ac.ebi.biosamples.legacy.json.controller.SamplesRelationsController;
import uk.ac.ebi.biosamples.legacy.json.domain.SamplesRelations;

@Service
public class SampleRelationsResourceAssembler
    implements ResourceAssembler<SamplesRelations, Resource<SamplesRelations>> {

  private EntityLinks entityLinks;

  public SampleRelationsResourceAssembler(EntityLinks entityLinks) {
    this.entityLinks = entityLinks;
  }

  @Override
  public Resource<SamplesRelations> toResource(SamplesRelations entity) {

    Resource<SamplesRelations> resource = new Resource<>(entity);
    resource.add(
        entityLinks.linkToSingleResource(SamplesRelations.class, entity.accession()).withSelfRel());
    resource.add(
        linkTo(methodOn(SamplesController.class).sampleByAccession(entity.accession()))
            .withRel("details"));
    resource.add(
        entityLinks
            .linkToSingleResource(SamplesRelations.class, entity.accession())
            .withRel("samplerelations"));

    resource.add(
        linkTo(
                methodOn(SamplesRelationsController.class)
                    .getSamplesGroupRelations(entity.accession()))
            .withRel("groups"));
    resource.add(
        linkTo(
                methodOn(SamplesRelationsController.class)
                    .getSamplesRelations(entity.accession(), "derivedFrom"))
            .withRel("derivedFrom"));
    resource.add(
        linkTo(
                methodOn(SamplesRelationsController.class)
                    .getSamplesRelations(entity.accession(), "derivedTo"))
            .withRel("derivedTo"));
    resource.add(
        linkTo(
                methodOn(SamplesRelationsController.class)
                    .getSamplesRelations(entity.accession(), "recuratedFrom"))
            .withRel("recuratedFrom"));
    resource.add(
        linkTo(
                methodOn(SamplesRelationsController.class)
                    .getSamplesRelations(entity.accession(), "childOf"))
            .withRel("childOf"));
    resource.add(
        linkTo(
                methodOn(SamplesRelationsController.class)
                    .getSamplesRelations(entity.accession(), "sameAs"))
            .withRel("sameAs"));
    resource.add(
        linkTo(
                methodOn(SamplesRelationsController.class)
                    .getSamplesRelations(entity.accession(), "parentOf"))
            .withRel("parentOf"));
    resource.add(
        linkTo(
                methodOn(SamplesRelationsController.class)
                    .getSamplesRelations(entity.accession(), "recuratedTo"))
            .withRel("recuratedTo"));
    resource.add(
        linkTo(
                methodOn(SamplesRelationsController.class)
                    .getSamplesRelations(entity.accession(), "externalLinks"))
            .withRel("externalLinks"));

    return resource;
  }
}
