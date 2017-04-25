package uk.ac.ebi.biosamples.controller;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.ExternalReferenceLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.ExternalReferenceLinkResourceAssembler;
import uk.ac.ebi.biosamples.service.ExternalReferenceLinkService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;

@RestController
@ExposesResourceFor(ExternalReferenceLink.class)
@RequestMapping("/externalreferencelinks")
public class ExternalReferenceLinkRestController {

	private final SampleService sampleService;
	private final ExternalReferenceLinkService externalReferenceLinkService;
	
	private final SampleResourceAssembler sampleResourceAssembler;
	private final ExternalReferenceLinkResourceAssembler externalReferenceLinkResourceAssembler;

	private final EntityLinks entityLinks;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public ExternalReferenceLinkRestController(SampleService sampleService, 
			ExternalReferenceLinkService externalReferenceService,
			SampleResourceAssembler sampleResourceAssembler,
			ExternalReferenceLinkResourceAssembler externalReferenceResourceAssembler,
			EntityLinks entityLinks) {
		
		this.sampleService = sampleService;
		this.entityLinks = entityLinks;
		this.sampleResourceAssembler = sampleResourceAssembler;
		this.externalReferenceLinkService = externalReferenceService;
		this.externalReferenceLinkResourceAssembler = externalReferenceResourceAssembler;
	}
	
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<PagedResources<Resource<ExternalReferenceLink>>> getPagedHal(
			Pageable page,
			PagedResourcesAssembler<ExternalReferenceLink> pageAssembler) {
    	Page<ExternalReferenceLink> pageExternalReference = externalReferenceLinkService.getPage(page);
		PagedResources<Resource<ExternalReferenceLink>> pagedResources = pageAssembler.toResource(pageExternalReference, externalReferenceLinkResourceAssembler,
				entityLinks.linkToCollectionResource(ExternalReferenceLink.class));
		
		return ResponseEntity.ok()
				.body(pagedResources);
    }
	
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value="/{id}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Resource<ExternalReferenceLink>> getExternalReferenceLinkHal(@PathVariable String id) {
    	ExternalReferenceLink externalReference = externalReferenceLinkService.getExternalReferenceLink(id);
    	if (externalReference == null) {
    		return ResponseEntity.notFound().build();
    	}
    	Resource<ExternalReferenceLink> resource = externalReferenceLinkResourceAssembler.toResource(externalReference);
    	//TODO add link to samples that use this link
		return ResponseEntity.ok()
				.body(resource);
    }

	
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value="/{id}/samples", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<PagedResources<Resource<Sample>>> getExternalReferenceSamplesHal(@PathVariable String id,
			Pageable pageable,
			PagedResourcesAssembler<Sample> pageAssembler) {

    	//get the response as if we'd called the externalReference endpoint
    	ResponseEntity<Resource<ExternalReferenceLink>> externalReferenceResponse = getExternalReferenceLinkHal(id);
    	if (!externalReferenceResponse.getStatusCode().is2xxSuccessful()) {
    		//propagate any non-2xx status code from /{id}/ to this endpoint
    		return ResponseEntity.status(externalReferenceResponse.getStatusCode()).build();
    	}

    	//get the content from the services
    	Page<Sample> pageSample = sampleService.getSamplesOfExternalReference(id, pageable);    	
    	
    	//use the resource assembler and a link to this method to build out the response content
		PagedResources<Resource<Sample>> pagedResources = pageAssembler.toResource(pageSample, sampleResourceAssembler,
				ControllerLinkBuilder.linkTo(ControllerLinkBuilder
						.methodOn(ExternalReferenceLinkRestController.class).getExternalReferenceSamplesHal(id, pageable, pageAssembler))
						.withRel("samples"));
		
		return ResponseEntity.ok()
				.body(pagedResources);
    	
    }
}
