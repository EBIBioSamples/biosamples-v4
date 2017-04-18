package uk.ac.ebi.biosamples.controller;

import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.Sample;

@RestController
@RequestMapping
public class RootHalController {

    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE })
	public ResponseEntity<ResourceSupport> rootHal() {
    	ResourceSupport resource = new ResourceSupport();
    	resource.add(ControllerLinkBuilder.linkTo(
				ControllerLinkBuilder.methodOn(SampleRestController.class)
				.searchHal(null,null,null,null))
			.withRel("samples"));
    	
    	return ResponseEntity.ok().body(resource);
    }
			
}
