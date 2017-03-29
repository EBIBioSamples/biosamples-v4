package uk.ac.ebi.biosamples.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SampleFacets;
import uk.ac.ebi.biosamples.service.SampleService;

@RestController
@RequestMapping(produces = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE})
@ExposesResourceFor(Sample.class)
public class FacetRestController {

	private SampleService sampleService;
	public FacetRestController(@Autowired SampleService sampleService) {
		this.sampleService = sampleService;
	}

    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value = "/facets/samples", produces = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE })
	public ResponseEntity<SampleFacets> search(
			@RequestParam(name="text", required=false) String text,
			@RequestParam(name="filter", required=false) String[] filters) {

		SampleFacets sampleFacets = sampleService.getFacets(text, sampleService.getFilters(filters), 10, 10);
		//PagedResources<Resource<SampleFacet>> paged = new PagedResources<Resource<SampleFacet>>(sampleFacets, null);
		//TODO use proper hateoas resource(s) and links and pageing
		return ResponseEntity.ok()
				.body(sampleFacets);
	}
}
