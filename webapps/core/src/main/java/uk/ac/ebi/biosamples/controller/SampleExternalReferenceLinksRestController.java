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

import uk.ac.ebi.biosamples.model.ExternalReferenceLink;
import uk.ac.ebi.biosamples.service.ExternalReferenceLinkResourceAssembler;
import uk.ac.ebi.biosamples.service.ExternalReferenceLinkService;

@RestController
@RequestMapping("/samples/{accession}/externalreferencelinks")
public class SampleExternalReferenceLinksRestController {

	private final EntityLinks entityLinks;
	private final ExternalReferenceLinkService externalReferenceLinkService;
	private final ExternalReferenceLinkResourceAssembler externalReferenceLinkResourceAssembler;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public SampleExternalReferenceLinksRestController(EntityLinks entityLinks, ExternalReferenceLinkService externalReferenceLinkService,
			ExternalReferenceLinkResourceAssembler externalReferenceLinkResourceAssembler) {
		this.entityLinks = entityLinks;
		this.externalReferenceLinkService = externalReferenceLinkService;
		this.externalReferenceLinkResourceAssembler = externalReferenceLinkResourceAssembler;
	}
    

    @CrossOrigin
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<PagedResources<Resource<ExternalReferenceLink>>> getExternalReferenceLinksJson(
			@PathVariable String accession,
			Pageable pageable,
			PagedResourcesAssembler<ExternalReferenceLink> pageAssembler) {
    	
    	Page<ExternalReferenceLink> page = externalReferenceLinkService.getExternalReferenceLinksForSample(accession, pageable);
    	
		//add the links to each individual sample on the page
		//also adds links to first/last/next/prev at the same time
		PagedResources<Resource<ExternalReferenceLink>> pagedResources = pageAssembler.toResource(page, externalReferenceLinkResourceAssembler,
				ControllerLinkBuilder.linkTo(
						ControllerLinkBuilder.methodOn(SampleExternalReferenceLinksRestController.class)
							.getExternalReferenceLinksJson(accession, pageable, pageAssembler)).withSelfRel());
    	
    	
		return ResponseEntity.ok(pagedResources);    
	}}
