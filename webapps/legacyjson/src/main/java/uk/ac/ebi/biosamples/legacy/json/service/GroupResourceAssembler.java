package uk.ac.ebi.biosamples.legacy.json.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.legacy.json.domain.GroupsRelations;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacyGroup;

@Service
public class GroupResourceAssembler implements ResourceAssembler<LegacyGroup, Resource<LegacyGroup>> {

    private final EntityLinks entityLinks;

    public GroupResourceAssembler(EntityLinks entityLinks) {
        this.entityLinks = entityLinks;
    }

    @Override
    public Resource<LegacyGroup> toResource(LegacyGroup entity) {

        Resource<LegacyGroup> resource = new Resource<>(entity);

        resource.add(entityLinks.linkToSingleResource(LegacyGroup.class, entity.accession()).withSelfRel());
        resource.add(entityLinks.linkToSingleResource(LegacyGroup.class, entity.accession()).withRel("group"));
        resource.add(entityLinks.linkToSingleResource(GroupsRelations.class, entity.accession()).withRel("relations"));

        return resource;
    }
}
