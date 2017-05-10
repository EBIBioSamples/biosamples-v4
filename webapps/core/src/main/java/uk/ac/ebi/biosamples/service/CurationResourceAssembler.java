package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.controller.CurationRestController;
import uk.ac.ebi.biosamples.model.Curation;

@Service
public class CurationResourceAssembler implements ResourceAssembler<Curation, Resource<Curation>> {

	private final EntityLinks entityLinks;
	
	public CurationResourceAssembler(EntityLinks entityLinks) {
		this.entityLinks = entityLinks;
	}
	@Override
	public Resource<Curation> toResource(Curation curation) {
		Resource<Curation> resource = new Resource<>(curation);
		
		resource.add(entityLinks.linkToSingleResource(Curation.class, curation.getHash()).withSelfRel());

		resource.add(ControllerLinkBuilder.linkTo(
				ControllerLinkBuilder.methodOn(CurationRestController.class)
					.getCurationSamplesHal(curation.getHash(), null, null))
				.withRel("samples"));
		
		return resource;
	}

}
