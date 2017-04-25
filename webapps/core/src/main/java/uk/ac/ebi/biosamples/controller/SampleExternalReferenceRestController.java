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

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.service.ExternalReferenceResourceAssembler;
import uk.ac.ebi.biosamples.service.ExternalReferenceService;

@RestController
@RequestMapping("/samples/{accession}/externalreferences")
public class SampleExternalReferenceRestController {

	private final EntityLinks entityLinks;
	private final ExternalReferenceService externalReferenceService;
	private final ExternalReferenceResourceAssembler externalReferenceResourceAssembler;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public SampleExternalReferenceRestController(EntityLinks entityLinks, ExternalReferenceService externalReferenceService,
			ExternalReferenceResourceAssembler externalReferenceResourceAssembler) {
		this.entityLinks = entityLinks;
		this.externalReferenceService = externalReferenceService;
		this.externalReferenceResourceAssembler = externalReferenceResourceAssembler;
	}
    

    @CrossOrigin
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<PagedResources<Resource<ExternalReference>>> getExternalReferencesJson(
			@PathVariable String accession,
			Pageable pageable,
			PagedResourcesAssembler<ExternalReference> pageAssembler) {
    	
    	Page<ExternalReference> page = externalReferenceService.getExternalReferencesForSample(accession, pageable);
    	
		//add the links to each individual sample on the page
		//also adds links to first/last/next/prev at the same time
		PagedResources<Resource<ExternalReference>> pagedResources = pageAssembler.toResource(page, externalReferenceResourceAssembler,
				ControllerLinkBuilder.linkTo(
						ControllerLinkBuilder.methodOn(SampleExternalReferenceRestController.class)
							.getExternalReferencesJson(accession, pageable, pageAssembler)).withSelfRel());
    	
    	
		return ResponseEntity.ok(pagedResources);    
	}}
