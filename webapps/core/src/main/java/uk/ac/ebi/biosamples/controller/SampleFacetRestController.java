package uk.ac.ebi.biosamples.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.SampleFacet;
import uk.ac.ebi.biosamples.service.SampleService;

@RestController
@ExposesResourceFor(SampleFacet.class)
@RequestMapping("/samples/facets")
public class SampleFacetRestController {

	@Autowired
	private SampleService sampleService;

    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<List<SampleFacet>> getFacetsJson(
			@RequestParam(name="text", required=false) String text,
			@RequestParam(name="filter", required=false) String[] filters) {
    	
    	//TODO support rows and start parameters

    	List<SampleFacet> sampleFacets = sampleService.getFacets(text, sampleService.getFilters(filters), 10, 10);
		//PagedResources<Resource<SampleFacet>> paged = new PagedResources<Resource<SampleFacet>>(sampleFacets, null);
		//TODO use proper hateoas resource(s) and links and pageing
    	//TODO as resource
		return ResponseEntity.ok()
				.body(sampleFacets);
	}
}
