package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;

@RestController
@RequestMapping("/externalreferences/{urlhash}/samples")
public class ExternalReferenceSampleRestController {

	private final EntityLinks entityLinks;
	private final SampleService sampleService;
	private final SampleResourceAssembler sampleResourceAssembler;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public ExternalReferenceSampleRestController(EntityLinks entityLinks, SampleService externalReferenceLinkService,
			SampleResourceAssembler externalReferenceLinkResourceAssembler) {
		this.entityLinks = entityLinks;
		this.sampleService = externalReferenceLinkService;
		this.sampleResourceAssembler = externalReferenceLinkResourceAssembler;
	}
    

    @CrossOrigin
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<PagedResources<Resource<Sample>>> getSamplesHal(
			@PathVariable String urlhash,
			Pageable pageable,
			PagedResourcesAssembler<Sample> pageAssembler) {
    	
    	Page<Sample> page = sampleService.getSamplesOfExternalReference(urlhash, pageable);
    	
		//add the links to each individual sample on the page
		//also adds links to first/last/next/prev at the same time
		PagedResources<Resource<Sample>> pagedResources = pageAssembler.toResource(page, sampleResourceAssembler,
				ControllerLinkBuilder.linkTo(
						ControllerLinkBuilder.methodOn(ExternalReferenceSampleRestController.class)
							.getSamplesHal(urlhash, pageable, pageAssembler)).withSelfRel());
    	
    	
		return ResponseEntity.ok(pagedResources);    
	}}
