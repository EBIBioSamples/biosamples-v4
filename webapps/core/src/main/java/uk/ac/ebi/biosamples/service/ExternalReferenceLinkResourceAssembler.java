package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.model.ExternalReferenceLink;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class ExternalReferenceLinkResourceAssembler
		implements ResourceAssembler<ExternalReferenceLink, Resource<ExternalReferenceLink>> {

	private final EntityLinks entityLinks;

	public ExternalReferenceLinkResourceAssembler(EntityLinks entityLinks) {
		this.entityLinks = entityLinks;
	}

	@Override
	public Resource<ExternalReferenceLink> toResource(ExternalReferenceLink externalRefrenceLink) {
		Resource<ExternalReferenceLink> resource = new Resource<>(externalRefrenceLink);

		resource.add(entityLinks.linkToSingleResource(ExternalReferenceLink.class, externalRefrenceLink.getId()).withSelfRel());
		
		return resource;
	}

}
