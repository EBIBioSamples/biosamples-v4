package uk.ac.ebi.biosamples.controller;

import java.net.URI;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.ExternalReferenceResourceAssembler;
import uk.ac.ebi.biosamples.service.ExternalReferenceService;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;

@RestController
@RequestMapping("/samples/{id}/externalreferences")
public class SampleExternalReferenceRestController {

	private final SampleService sampleService;
	private final ExternalReferenceService externalReferenceService;
	
	private final ExternalReferenceResourceAssembler externalReferenceResourceAssembler;
	
	private final EntityLinks entityLinks;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public SampleExternalReferenceRestController(SampleService sampleService,
			ExternalReferenceService externalReferenceService,
			ExternalReferenceResourceAssembler externalReferenceResourceAssembler,
			EntityLinks entityLinks) {
		this.sampleService = sampleService;
		this.externalReferenceService = externalReferenceService;
		this.externalReferenceResourceAssembler = externalReferenceResourceAssembler;
		this.entityLinks = entityLinks;
	}


    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE  })
	public ResponseEntity<PagedResources<Resource<ExternalReference>>> getSampleExternalReferencesHal(@PathVariable String id,
			Pageable pageable,
			PagedResourcesAssembler<ExternalReference> pageAssembler) {

		Sample sample = null;
		try {
			sample = sampleService.fetch(id);
		} catch (IllegalArgumentException e) {
			// did not exist, throw 404
			return ResponseEntity.notFound().build();
		}
		
		if (sample.getName() == null) {
			//if it has no name, then its just created by accessioning or reference
			//can't read it, but could put to it
			//TODO make sure "options" is correct for this
			return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
		}
		
		// check if the release date is in the future and if so return it as private
		if (sample.getRelease().isAfter(LocalDateTime.now())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
    	
    	//get the content from the services
    	Page<ExternalReference> pageExternalReference = externalReferenceService.getExternalReferencesOfSample(id, pageable);
    	
    	//use the resource assembler and a link to this method to build out the response content
		PagedResources<Resource<ExternalReference>> pagedResources = pageAssembler.toResource(pageExternalReference, externalReferenceResourceAssembler,
				ControllerLinkBuilder.linkTo(ControllerLinkBuilder
						.methodOn(SampleExternalReferenceRestController.class).getSampleExternalReferencesHal(id, pageable, pageAssembler))
						.withRel("externalreferences"));
		
		return ResponseEntity.ok()
				.body(pagedResources);
    }
    

	@PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Resource<ExternalReference>> postJson(@PathVariable String id, 
			@RequestBody ExternalReference externalReference) {
		
		log.info("Recieved POST");
		externalReference = externalReferenceService.store(externalReference, id);
		Resource<ExternalReference> externalReferenceResource = externalReferenceResourceAssembler.toResource(externalReference);
		
		// create the response object with the appropriate status
		return ResponseEntity.created(URI.create(externalReferenceResource.getLink("self").getHref()))
				.body(externalReferenceResource);
	}
}
