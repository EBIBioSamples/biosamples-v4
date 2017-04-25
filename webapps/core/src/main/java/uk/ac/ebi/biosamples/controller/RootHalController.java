package uk.ac.ebi.biosamples.controller;

import org.springframework.hateoas.EntityLinks;
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

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.ExternalReferenceLink;
import uk.ac.ebi.biosamples.model.Sample;

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
    	
    	resource.add(entityLinks.linkToCollectionResource(Sample.class).withRel("samples"));
    	resource.add(entityLinks.linkToCollectionResource(ExternalReference.class).withRel("externalReferences"));
    	resource.add(entityLinks.linkToCollectionResource(ExternalReferenceLink.class).withRel("externalReferenceLinks"));
    	
    	return ResponseEntity.ok().body(resource);
    }
			
}
