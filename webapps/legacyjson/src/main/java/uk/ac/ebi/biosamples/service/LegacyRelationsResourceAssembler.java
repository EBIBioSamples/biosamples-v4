package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.LegacyRelations;
import uk.ac.ebi.biosamples.model.LegacySample;

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


        return resource;
    }
}
