package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.controller.SampleExternalReferenceLinksRestController;
import uk.ac.ebi.biosamples.model.ExternalReference;
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

		resource.add(ControllerLinkBuilder.linkTo(
				ControllerLinkBuilder.methodOn(SampleExternalReferenceLinksRestController.class)
					.getExternalReferenceLinkJson(externalRefrenceLink.getSample(), externalRefrenceLink.getId())).withSelfRel());
		
		resource.add(entityLinks.linkToSingleResource(Sample.class, externalRefrenceLink.getSample())
				.withRel("sample"));
		resource.add(entityLinks.linkToSingleResource(ExternalReference.class, externalRefrenceLink.getUrlHash())
				.withRel("externalreference"));
		
		return resource;
	}

}
