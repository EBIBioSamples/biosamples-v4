package uk.ac.ebi.biosamples.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.service.SampleService;

@RestController
@ExposesResourceFor(Autocomplete.class)
@RequestMapping("/samples/autocomplete")
public class SampleAutocompleteRestController {

	@Autowired
	private SampleService sampleService;
    
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Autocomplete> getAutocompleteJson(
			@RequestParam(name="text", required=false) String text,
			@RequestParam(name="filter", required=false) String[] filters,
			@RequestParam(name="rows", defaultValue="10") Integer rows) {
		MultiValueMap<String, String> filtersMap = sampleService.getFilters(filters);
    	Autocomplete autocomplete = sampleService.getAutocomplete(text, filtersMap, rows);
    	//TODO as resource
		return ResponseEntity.ok().body(autocomplete);
	}
}
