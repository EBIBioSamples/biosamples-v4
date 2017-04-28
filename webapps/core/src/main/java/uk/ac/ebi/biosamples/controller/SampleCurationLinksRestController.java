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

import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.ExternalReferenceLink;
import uk.ac.ebi.biosamples.service.CurationLinkResourceAssembler;
import uk.ac.ebi.biosamples.service.CurationResourceAssembler;
import uk.ac.ebi.biosamples.service.CurationService;
import uk.ac.ebi.biosamples.service.ExternalReferenceLinkResourceAssembler;
import uk.ac.ebi.biosamples.service.ExternalReferenceResourceAssembler;
import uk.ac.ebi.biosamples.service.ExternalReferenceService;

@RestController
@RequestMapping("/samples/{accession}/curationlinks")
public class SampleCurationLinksRestController {

	private final CurationService curationService;
	private final CurationLinkResourceAssembler curationLinkResourceAssembler;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public SampleCurationLinksRestController(CurationService curationService,
			CurationLinkResourceAssembler curationLinkResourceAssembler) {
		this.curationService = curationService;
		this.curationLinkResourceAssembler = curationLinkResourceAssembler;
	}
    
    @CrossOrigin
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<PagedResources<Resource<CurationLink>>> getCurationLinkPageJson(
			@PathVariable String accession,
			Pageable pageable,
			PagedResourcesAssembler<CurationLink> pageAssembler) {
    	
    	Page<CurationLink> page = curationService.getCurationLinksForSample(accession, pageable);
    	
		//add the links to each individual sample on the page
		//also adds links to first/last/next/prev at the same time
		PagedResources<Resource<CurationLink>> pagedResources = pageAssembler.toResource(page, curationLinkResourceAssembler,
			ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SampleCurationLinksRestController.class)
				.getCurationLinkPageJson(accession, pageable, pageAssembler)).withSelfRel());
    	
		return ResponseEntity.ok(pagedResources);    
	}
    

    @CrossOrigin
	@GetMapping(value = "/{id}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<Resource<CurationLink>> getCurationLinkJson(
			@PathVariable String accession,
			@PathVariable String id) {
    	
    	CurationLink curationLink = curationService.getCurationLink(id);    	
    	Resource<CurationLink> resource = curationLinkResourceAssembler.toResource(curationLink);
    	
		return ResponseEntity.ok(resource);   
	}
}
