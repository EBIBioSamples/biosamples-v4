package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.api.LegacyJsonSamplesRelationsController;
import uk.ac.ebi.biosamples.model.LegacyRelations;
import uk.ac.ebi.biosamples.model.LegacySample;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Service
public class LegacyRelationsResourceAssembler implements ResourceAssembler<LegacyRelations, Resource<LegacyRelations>>{

    private EntityLinks entityLinks;

    public LegacyRelationsResourceAssembler(EntityLinks entityLinks) {
        this.entityLinks = entityLinks;
    }

    @Override
    public Resource<LegacyRelations> toResource(LegacyRelations entity) {

        Resource<LegacyRelations> resource = new Resource<>(entity);

        resource.add(entityLinks.linkToSingleResource(LegacyRelations.class, entity.accession()).withSelfRel());
        resource.add(entityLinks.linkToSingleResource(LegacySample.class, entity.accession()).withRel("details"));
        resource.add(entityLinks.linkToSingleResource(LegacyRelations.class, entity.accession()).withRel("samplerelations"));
        resource.add(linkTo(methodOn(LegacyJsonSamplesRelationsController.class).getSamplesGroupRelations(entity.accession())).withRel("groups"));
        resource.add(new Link("test").withRel("derivedFrom"));
        resource.add(new Link("test").withRel("recuratedFrom"));
        resource.add(new Link("test").withRel("childOf"));
        resource.add(new Link("test").withRel("sameAs"));
        resource.add(new Link("test").withRel("parentOf"));
        resource.add(new Link("test").withRel("derivedTo"));
        resource.add(new Link("test").withRel("recuratedTo"));
        resource.add(new Link("test").withRel("externallinks"));


        return resource;
    }
}
