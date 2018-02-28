package uk.ac.ebi.biosamples.controller;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.service.SampleService;

@RestController
@ExposesResourceFor(Autocomplete.class)
@RequestMapping("/samples/autocomplete")
public class SampleAutocompleteRestController {

	private final SampleService sampleService;
	private final FilterService filterService;

	private final EntityLinks entityLinks;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public SampleAutocompleteRestController(SampleService sampleService, FilterService filterService,
			EntityLinks entityLinks) {
		this.sampleService = sampleService;
		this.filterService = filterService;
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
		Collection<Filter> filters = filterService.getFiltersCollection(filter);
    	Autocomplete autocomplete = sampleService.getAutocomplete(text, filters, rows);
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
			ControllerLinkBuilder.methodOn(SamplesRestController.class)
					.searchHal(text, filter, null, null, null, null, null))
    			.withRel("samples"));
		
		return ResponseEntity.ok().body(resource);
	}
    
	public static Link getLink(String text, String[] filter, Integer size, String rel) {
		UriComponentsBuilder builder = ControllerLinkBuilder.linkTo(SampleAutocompleteRestController.class)
				.toUriComponentsBuilder();
		if (text != null && text.trim().length() > 0) {
			builder.queryParam("text", text);
		}
		if (filter != null) {
			for (String filterString : filter) {
				builder.queryParam("filter",filterString);				
			}
		}
		if (size != null) {
			builder.queryParam("size", size);
		}
		return new Link(builder.toUriString(), rel);
	}
}
