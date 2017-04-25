package uk.ac.ebi.biosamples.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.hateoas.mvc.ControllerLinkBuilder.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.ExternalReferenceLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SampleFacet;
import uk.ac.ebi.biosamples.service.ExternalReferenceLinkResourceAssembler;
import uk.ac.ebi.biosamples.service.ExternalReferenceLinkService;
import uk.ac.ebi.biosamples.service.ExternalReferenceResourceAssembler;
import uk.ac.ebi.biosamples.service.ExternalReferenceService;
import uk.ac.ebi.biosamples.service.FacetService;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.service.SampleService;

@RestController
@RequestMapping("/samples/{accession}/externalreferences")
public class SampleExternalReferencesRestController {

	private final EntityLinks entityLinks;
	private final ExternalReferenceService externalReferenceService;
	private final ExternalReferenceResourceAssembler externalReferenceResourceAssembler;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public SampleExternalReferencesRestController(EntityLinks entityLinks, ExternalReferenceService externalReferenceService,
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
						ControllerLinkBuilder.methodOn(SampleExternalReferencesRestController.class)
							.getExternalReferencesJson(accession, pageable, pageAssembler)).withSelfRel());
    	
    	
		return ResponseEntity.ok(pagedResources);    
	}}
