package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.controller.SampleCurationLinksRestController;
import uk.ac.ebi.biosamples.controller.SampleRestController;
import uk.ac.ebi.biosamples.controller.SamplesRestController;
import uk.ac.ebi.biosamples.model.Sample;

/**
 * This class is used by Spring to add HAL _links for {@Link Sample} objects.
 * 
 * @author faulcon
 *
 */
@Service
public class SampleResourceAssembler implements ResourceAssembler<Sample, Resource<Sample>> {

	private final EntityLinks entityLinks;
	
	public SampleResourceAssembler(EntityLinks entityLinks) {
		this.entityLinks = entityLinks;
	}

	@Override
	public Resource<Sample> toResource(Sample sample) {
		Resource<Sample> resource = new Resource<>(sample);
		
		resource.add(ControllerLinkBuilder.linkTo(SampleRestController.class, sample.getAccession()).withSelfRel());
							
		resource.add(ControllerLinkBuilder.linkTo(
				ControllerLinkBuilder.methodOn(SampleCurationLinksRestController.class)
					.getCurationLinkPageJson(sample.getAccession(), null, null)).withRel("curationLinks"));
		
		return resource;
	}

}
