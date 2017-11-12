package uk.ac.ebi.biosamples.legacy.json.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.legacy.json.controller.SamplesRelationsController;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacySample;
import uk.ac.ebi.biosamples.legacy.json.domain.SamplesRelations;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Service
public class SampleRelationsResourceAssembler implements ResourceAssembler<SamplesRelations, Resource<SamplesRelations>>{

    private EntityLinks entityLinks;

    public SampleRelationsResourceAssembler(EntityLinks entityLinks) {
        this.entityLinks = entityLinks;
    }

    @Override
    public Resource<SamplesRelations> toResource(SamplesRelations entity) {

        Resource<SamplesRelations> resource = new Resource<>(entity);
        resource.add(entityLinks.linkToSingleResource(SamplesRelations.class, entity.accession()).withSelfRel());
        resource.add(entityLinks.linkToSingleResource(LegacySample.class, entity.accession()).withRel("details"));
        resource.add(entityLinks.linkToSingleResource(SamplesRelations.class, entity.accession()).withRel("samplerelations"));
        resource.add(linkTo(methodOn(SamplesRelationsController.class).getSamplesGroupRelations(entity.accession())).withRel("groups"));
        resource.add(linkTo(methodOn(SamplesRelationsController.class).getSamplesRelations(entity.accession(), "derivedFrom")).withRel("derivedFrom"));
        resource.add(linkTo(methodOn(SamplesRelationsController.class).getSamplesRelations(entity.accession(), "derivedTo")).withRel("derivedTo"));
        resource.add(linkTo(methodOn(SamplesRelationsController.class).getSamplesRelations(entity.accession(), "recuratedFrom")).withRel("recuratedFrom"));
        resource.add(linkTo(methodOn(SamplesRelationsController.class).getSamplesRelations(entity.accession(), "childOf")).withRel("childOf"));
        resource.add(linkTo(methodOn(SamplesRelationsController.class).getSamplesRelations(entity.accession(), "sameAs")).withRel("sameAs"));
        resource.add(linkTo(methodOn(SamplesRelationsController.class).getSamplesRelations(entity.accession(), "parentOf")).withRel("parentOf"));
        resource.add(linkTo(methodOn(SamplesRelationsController.class).getSamplesRelations(entity.accession(), "recuratedTo")).withRel("recuratedTo"));
        resource.add(linkTo(methodOn(SamplesRelationsController.class).getSamplesRelations(entity.accession(), "externallinks")).withRel("externallinks"));

        return resource;
    }
}
