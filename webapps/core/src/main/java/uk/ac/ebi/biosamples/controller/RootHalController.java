package uk.ac.ebi.biosamples.controller;

import java.util.concurrent.TimeUnit;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class RootHalController {
	
	private final EntityLinks entityLinks;
	
	public RootHalController(EntityLinks entityLinks) {
		this.entityLinks = entityLinks;
	}

    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE })
	public ResponseEntity<ResourceSupport> rootHal() {
    	ResourceSupport resource = new ResourceSupport();    	
    	    	
    	resource.add(ControllerLinkBuilder.linkTo(SamplesRestController.class).withRel("samples"));
    	resource.add(ControllerLinkBuilder.linkTo(CurationRestController.class).withRel("curations"));
    	
    	return ResponseEntity.ok()
				.header(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(60, TimeUnit.MINUTES).cachePublic().getHeaderValue())
				.body(resource);
    }
			
}
