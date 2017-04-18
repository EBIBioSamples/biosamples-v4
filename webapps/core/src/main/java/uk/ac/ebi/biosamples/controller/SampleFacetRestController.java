package uk.ac.ebi.biosamples.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SampleFacet;
import uk.ac.ebi.biosamples.service.SampleService;

@RestController
@ExposesResourceFor(SampleFacet.class)
@RequestMapping("/samples/facets")
public class SampleFacetRestController {

	private final SampleService sampleService;

	private final EntityLinks entityLinks;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public SampleFacetRestController(SampleService sampleService,
			EntityLinks entityLinks) {
		this.sampleService = sampleService;
		this.entityLinks = entityLinks;
	}
    

    @CrossOrigin
	@GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<Collection<SampleFacet>> getFacetsJson(
			@RequestParam(name="text", required=false) String text,
			@RequestParam(name="filter", required=false) String[] filter) {
    	ResponseEntity<Resources<SampleFacet>> halResponse = getFacetsHal(text,filter);
		return ResponseEntity.status(halResponse.getStatusCode()).headers(halResponse.getHeaders()).body(halResponse.getBody().getContent());    
	}

    @CrossOrigin
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE})
	public ResponseEntity<Resources<SampleFacet>> getFacetsHal(
			@RequestParam(name="text", required=false) String text,
			@RequestParam(name="filter", required=false) String[] filter) {
    	
    	//TODO support rows and start parameters

    	List<SampleFacet> sampleFacets = sampleService.getFacets(text, sampleService.getFilters(filter), 10, 10);    	
    	Resources<SampleFacet> resources = new Resources<>(sampleFacets);
    	
		//Links for the entire page
		//this is hacky, but no clear way to do this in spring-hateoas currently
    	resources.removeLinks();
    			
    	//to generate the HAL template correctly, the parameter name must match the requestparam name
		resources.add(ControllerLinkBuilder.linkTo(
				ControllerLinkBuilder.methodOn(SampleFacetRestController.class)
					.getFacetsHal(text, filter))
				.withSelfRel());
		
		resources.add(ControllerLinkBuilder.linkTo(
				ControllerLinkBuilder.methodOn(SampleRestController.class)
					.searchHal(text, filter,null, null, null))
				.withRel("samples"));
		
		
		return ResponseEntity.ok().body(resources);
	}
}
