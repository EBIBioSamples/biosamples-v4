package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Service;

import uk.ac.ebi.biosamples.controller.SampleRestController;
import uk.ac.ebi.biosamples.model.Sample;

/**
 * This class is used by Spring to add HAL _links for {@Link Sample} objects.
 * 
 * @author faulcon
 *
 */
@Service
public class SampleResourceAssembler implements ResourceAssembler<Sample, Resource<Sample>> {

	public SampleResourceAssembler() {
	}

	@Override
	public Resource<Sample> toResource(Sample sample) {
		Resource<Sample> resource = new Resource<>(sample);

		resource.add(ControllerLinkBuilder
				.linkTo(ControllerLinkBuilder.methodOn(SampleRestController.class).getSampleHal(sample.getAccession()))
				.withSelfRel());
		return resource;
	}

}
