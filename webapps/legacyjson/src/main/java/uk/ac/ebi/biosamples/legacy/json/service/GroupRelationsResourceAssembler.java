package uk.ac.ebi.biosamples.legacy.json.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.legacy.json.domain.GroupsRelations;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacySample;

@Service
public class GroupRelationsResourceAssembler implements ResourceAssembler<GroupsRelations, Resource<GroupsRelations>>{

    private EntityLinks entityLinks;

    public GroupRelationsResourceAssembler(EntityLinks entityLinks) {
        this.entityLinks = entityLinks;
    }

    @Override
    public Resource<GroupsRelations> toResource(GroupsRelations entity) {

        Resource<GroupsRelations> resource = new Resource<>(entity);

        resource.add(entityLinks.linkToSingleResource(GroupsRelations.class, entity.accession()).withSelfRel());
        resource.add(entityLinks.linkToSingleResource(LegacySample.class, entity.accession()).withRel("details"));
        resource.add(entityLinks.linkToSingleResource(GroupsRelations.class, entity.accession()).withRel("groupsrelations"));
        resource.add(new Link("test").withRel("externallinks"));
        resource.add(new Link("test").withRel("samples"));

        return resource;
    }
}
