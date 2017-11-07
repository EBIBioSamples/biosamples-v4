package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.LegacyGroupsRelations;
import uk.ac.ebi.biosamples.model.LegacySample;

@Service
public class LegacyGroupsRelationsResourceAssembler implements ResourceAssembler<LegacyGroupsRelations, Resource<LegacyGroupsRelations>>{

    private EntityLinks entityLinks;

    public LegacyGroupsRelationsResourceAssembler(EntityLinks entityLinks) {
        this.entityLinks = entityLinks;
    }

    @Override
    public Resource<LegacyGroupsRelations> toResource(LegacyGroupsRelations entity) {

        Resource<LegacyGroupsRelations> resource = new Resource<>(entity);

        resource.add(entityLinks.linkToSingleResource(LegacyGroupsRelations.class, entity.accession()).withSelfRel());
        resource.add(entityLinks.linkToSingleResource(LegacySample.class, entity.accession()).withRel("details"));
        resource.add(entityLinks.linkToSingleResource(LegacyGroupsRelations.class, entity.accession()).withRel("groupsrelations"));
        resource.add(new Link("test").withRel("externallinks"));
        resource.add(new Link("test").withRel("samples"));



        return resource;
    }
}
