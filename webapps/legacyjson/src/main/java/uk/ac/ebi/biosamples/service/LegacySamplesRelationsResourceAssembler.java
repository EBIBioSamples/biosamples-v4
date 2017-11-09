package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.api.LegacyJsonSamplesRelationsController;
import uk.ac.ebi.biosamples.model.LegacySample;
import uk.ac.ebi.biosamples.model.LegacySamplesRelations;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Service
public class LegacySamplesRelationsResourceAssembler implements ResourceAssembler<LegacySamplesRelations, Resource<LegacySamplesRelations>>{

    private EntityLinks entityLinks;

    public LegacySamplesRelationsResourceAssembler(EntityLinks entityLinks) {
        this.entityLinks = entityLinks;
    }

    @Override
    public Resource<LegacySamplesRelations> toResource(LegacySamplesRelations entity) {

        Resource<LegacySamplesRelations> resource = new Resource<>(entity);
        resource.add(entityLinks.linkToSingleResource(LegacySamplesRelations.class, entity.accession()).withSelfRel());
        resource.add(entityLinks.linkToSingleResource(LegacySample.class, entity.accession()).withRel("details"));
        resource.add(entityLinks.linkToSingleResource(LegacySamplesRelations.class, entity.accession()).withRel("samplerelations"));
        resource.add(linkTo(methodOn(LegacyJsonSamplesRelationsController.class).getSamplesGroupRelations(entity.accession())).withRel("groups"));
        resource.add(linkTo(methodOn(LegacyJsonSamplesRelationsController.class).getSamplesRelations(entity.accession(), "derivedFrom")).withRel("derivedFrom"));
        resource.add(linkTo(methodOn(LegacyJsonSamplesRelationsController.class).getSamplesRelations(entity.accession(), "derivedTo")).withRel("derivedTo"));
        resource.add(linkTo(methodOn(LegacyJsonSamplesRelationsController.class).getSamplesRelations(entity.accession(), "recuratedFrom")).withRel("recuratedFrom"));
        resource.add(linkTo(methodOn(LegacyJsonSamplesRelationsController.class).getSamplesRelations(entity.accession(), "childOf")).withRel("childOf"));
        resource.add(linkTo(methodOn(LegacyJsonSamplesRelationsController.class).getSamplesRelations(entity.accession(), "sameAs")).withRel("sameAs"));
        resource.add(linkTo(methodOn(LegacyJsonSamplesRelationsController.class).getSamplesRelations(entity.accession(), "parentOf")).withRel("parentOf"));
        resource.add(linkTo(methodOn(LegacyJsonSamplesRelationsController.class).getSamplesRelations(entity.accession(), "recuratedTo")).withRel("recuratedTo"));
        resource.add(linkTo(methodOn(LegacyJsonSamplesRelationsController.class).getSamplesRelations(entity.accession(), "externallinks")).withRel("externallinks"));

        return resource;
    }
}
