package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.LegacySamplesRelations;
import uk.ac.ebi.biosamples.model.LegacySample;

@Service
public class LegacySampleResourceAssembler implements ResourceAssembler<LegacySample, Resource<LegacySample>> {

    private final EntityLinks entityLinks;

    public LegacySampleResourceAssembler(EntityLinks entityLinks) {
        this.entityLinks = entityLinks;
    }

    @Override
    public Resource<LegacySample> toResource(LegacySample entity) {

        Resource<LegacySample> resource = new Resource<>(entity);

        resource.add(entityLinks.linkToSingleResource(LegacySample.class, entity.accession()).withSelfRel());
        resource.add(entityLinks.linkToSingleResource(LegacySample.class, entity.accession()).withRel("sample"));
        resource.add(entityLinks.linkToSingleResource(LegacySamplesRelations.class, entity.accession()).withRel("relations"));

        return resource;
    }
}
