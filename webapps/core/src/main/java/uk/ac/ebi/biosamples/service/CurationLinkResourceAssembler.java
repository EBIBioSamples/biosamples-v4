package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.controller.SampleCurationLinksRestController;
import uk.ac.ebi.biosamples.controller.SampleExternalReferenceLinksRestController;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class CurationLinkResourceAssembler implements ResourceAssembler<CurationLink, Resource<CurationLink>> {

	private final EntityLinks entityLinks;
	
	public CurationLinkResourceAssembler(EntityLinks entityLinks) {
		this.entityLinks = entityLinks;
	}
	@Override
	public Resource<CurationLink> toResource(CurationLink curationLink) {
		Resource<CurationLink> resource = new Resource<>(curationLink);

		resource.add(ControllerLinkBuilder.linkTo(
				ControllerLinkBuilder.methodOn(SampleCurationLinksRestController.class)
					.getCurationLinkJson(curationLink.getSample(), curationLink.getHash())).withSelfRel());
		
		resource.add(entityLinks.linkToSingleResource(Sample.class, curationLink.getSample())
				.withRel("sample"));
		resource.add(entityLinks.linkToSingleResource(Curation.class, curationLink.getCuration().getHash())
				.withRel("curation"));
		
		return resource;
	}

}
