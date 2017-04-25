package uk.ac.ebi.biosamples.controller;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.SampleFacet;
import uk.ac.ebi.biosamples.service.FacetService;
import uk.ac.ebi.biosamples.service.FilterService;

@RestController
@ExposesResourceFor(SampleFacet.class)
@RequestMapping("/samples/facets")
public class SampleFacetRestController {

	private final FacetService facetService;
	private final FilterService filterService;

	private final EntityLinks entityLinks;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public SampleFacetRestController(FacetService facetService, FilterService filterService,
			EntityLinks entityLinks) {
		this.facetService = facetService;
		this.filterService = filterService;
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

    	List<SampleFacet> sampleFacets = facetService.getFacets(text, filterService.getFilters(filter), 10, 10);    	
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
					.searchHal(text, filter,null, null))
				.withRel("samples"));
		
		
		return ResponseEntity.ok().body(resources);
	}
}
