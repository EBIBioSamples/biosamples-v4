package uk.ac.ebi.biosamples.legacy.json.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.legacy.json.domain.ExternalLinksRelation;

@Service
public class ExternalLinksResourceAssembler implements ResourceAssembler<ExternalLinksRelation, Resource<ExternalLinksRelation>> {

    private final EntityLinks entityLinks;

    public ExternalLinksResourceAssembler(EntityLinks entityLinks) {
        this.entityLinks = entityLinks;
    }


    @Override
    public Resource<ExternalLinksRelation> toResource(ExternalLinksRelation entity) {
        Resource resource = new Resource(entity);
        resource.add(entityLinks.linkToSingleResource(ExternalLinksRelation.class, entity.url()).withSelfRel());
        resource.add(entityLinks.linkToSingleResource(ExternalLinksRelation.class, entity.url()).withRel("externallinkrelations"));
//        resource.add(new Link("test").withRel("samples"));
//        resource.add(new Link("test").withRel("groups"));
        return resource;
    }
}
