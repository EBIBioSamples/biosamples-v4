package uk.ac.ebi.biosamples.service;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.TemplateVariable.VariableType;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.collect.Maps;

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

	private Logger log = LoggerFactory.getLogger(getClass());

	private final EntityLinks entityLinks;
	
	public SampleResourceAssembler(EntityLinks entityLinks) {
		this.entityLinks = entityLinks;
	}

	@Override
	public Resource<Sample> toResource(Sample sample) {
		Resource<Sample> resource = new Resource<>(sample);
				
		resource.add(getSelfLink(sample));		
									
		resource.add();
		
		return resource;
	}
	
	private Link getSelfLink(Sample sample) {		
		UriComponentsBuilder builder = ControllerLinkBuilder.linkTo(SampleRestController.class, sample.getAccession())
				.toUriComponentsBuilder();
		log.info("builder.toUriString() = "+builder.toUriString());
		return new Link(builder.toUriString(), Link.REL_SELF);
	}
}
