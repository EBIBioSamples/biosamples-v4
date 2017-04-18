package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleService;

@RestController
@ExposesResourceFor(Autocomplete.class)
@RequestMapping("/samples/autocomplete")
public class SampleAutocompleteRestController {

	private final SampleService sampleService;

	private final EntityLinks entityLinks;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public SampleAutocompleteRestController(SampleService sampleService,
			EntityLinks entityLinks) {
		this.sampleService = sampleService;
		this.entityLinks = entityLinks;
	}
    
    @CrossOrigin
	@GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Autocomplete> getAutocompleteJson(
			@RequestParam(name="text", required=false) String text,
			@RequestParam(name="filter", required=false) String[] filter,
			@RequestParam(name="rows", defaultValue="10") Integer rows) {
    	ResponseEntity<Resource<Autocomplete>> halResponse = getAutocompleteHal(text,filter,rows);
		return ResponseEntity.status(halResponse.getStatusCode()).headers(halResponse.getHeaders()).body(halResponse.getBody().getContent());    	
	}
    
    @CrossOrigin
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE })
	public ResponseEntity<Resource<Autocomplete>> getAutocompleteHal(
			@RequestParam(name="text", required=false) String text,
			@RequestParam(name="filter", required=false) String[] filter,
			@RequestParam(name="rows", defaultValue="10") Integer rows) {
		MultiValueMap<String, String> filtersMap = sampleService.getFilters(filter);
    	Autocomplete autocomplete = sampleService.getAutocomplete(text, filtersMap, rows);
    	Resource<Autocomplete> resource = new Resource<>(autocomplete);

		//Links for the entire page
		//this is hacky, but no clear way to do this in spring-hateoas currently
    	resource.removeLinks();
    	//to generate the HAL template correctly, the parameter name must match the requestparam name
    	resource.add(ControllerLinkBuilder.linkTo(
				ControllerLinkBuilder.methodOn(SampleFacetRestController.class)
					.getFacetsHal(text, filter))
				.withSelfRel());
		
    	resource.add(ControllerLinkBuilder.linkTo(
				ControllerLinkBuilder.methodOn(SampleRestController.class)
					.searchHal(text, filter, null, null, null))
				.withRel("samples"));
		
		return ResponseEntity.ok().body(resource);
	}
}
