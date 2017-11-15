package uk.ac.ebi.biosamples.legacy.json.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.legacy.json.domain.SamplesRelations;
import uk.ac.ebi.biosamples.legacy.json.domain.LegacySample;

@Service
public class SampleResourceAssembler implements ResourceAssembler<LegacySample, Resource<LegacySample>> {

    private final EntityLinks entityLinks;

    public SampleResourceAssembler(EntityLinks entityLinks) {
        this.entityLinks = entityLinks;
    }

    @Override
    public Resource<LegacySample> toResource(LegacySample entity) {

        Resource<LegacySample> resource = new Resource<>(entity);

        resource.add(entityLinks.linkToSingleResource(LegacySample.class, entity.accession()).withSelfRel());
        resource.add(entityLinks.linkToSingleResource(LegacySample.class, entity.accession()).withRel("sample"));
        resource.add(entityLinks.linkToSingleResource(SamplesRelations.class, entity.accession()).withRel("relations"));

        return resource;
    }
}
