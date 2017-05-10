package uk.ac.ebi.biosamples.controller;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.ExternalReferenceLink;
import uk.ac.ebi.biosamples.service.ExternalReferenceLinkResourceAssembler;
import uk.ac.ebi.biosamples.service.ExternalReferenceService;

@RestController
@RequestMapping("/samples/{accession}/externalreferencelinks")
public class SampleExternalReferenceLinksRestController {

	private final ExternalReferenceService externalReferenceService;
	private final ExternalReferenceLinkResourceAssembler externalReferenceLinkResourceAssembler;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public SampleExternalReferenceLinksRestController(ExternalReferenceService externalReferenceService,
			ExternalReferenceLinkResourceAssembler externalReferenceLinkResourceAssembler) {
		this.externalReferenceService = externalReferenceService;
		this.externalReferenceLinkResourceAssembler = externalReferenceLinkResourceAssembler;
	}
    
    @CrossOrigin
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<PagedResources<Resource<ExternalReferenceLink>>> getExternalReferenceLinkPageJson(
			@PathVariable String accession,
			Pageable pageable,
			PagedResourcesAssembler<ExternalReferenceLink> pageAssembler) {
    	
    	Page<ExternalReferenceLink> page = externalReferenceService.getExternalReferenceLinksForSample(accession, pageable);
    	
		//add the links to each individual sample on the page
		//also adds links to first/last/next/prev at the same time
		PagedResources<Resource<ExternalReferenceLink>> pagedResources = pageAssembler.toResource(page, externalReferenceLinkResourceAssembler,
			ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SampleExternalReferenceLinksRestController.class)
				.getExternalReferenceLinkPageJson(accession, pageable, pageAssembler)).withSelfRel());
    	
		return ResponseEntity.ok(pagedResources);
	}
    

    @CrossOrigin
	@GetMapping(value = "/{id}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<Resource<ExternalReferenceLink>> getExternalReferenceLinkJson(
			@PathVariable String accession,
			@PathVariable String id) {
    	
    	ExternalReferenceLink externalReference = externalReferenceService.getExternalReferenceLink(id);    	
    	Resource<ExternalReferenceLink> resource = externalReferenceLinkResourceAssembler.toResource(externalReference);
    	
		return ResponseEntity.ok(resource);   
	}
    
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource<ExternalReferenceLink>> createExternalReferenceLinkJson(
			@PathVariable String accession,
			@RequestBody ExternalReference externalReference) {
		log.info("Recieved POST for " + accession + " : "+externalReference);
    	ExternalReferenceLink externalReferenceLink = ExternalReferenceLink.build(accession, externalReference.getUrl());
    	
    	externalReferenceLink = externalReferenceService.store(externalReferenceLink);
    	Resource<ExternalReferenceLink> resource = externalReferenceLinkResourceAssembler.toResource(externalReferenceLink);

		// create the response object with the appropriate status
		return ResponseEntity.created(URI.create(resource.getLink("self").getHref()))
				.body(resource);
    }
}
