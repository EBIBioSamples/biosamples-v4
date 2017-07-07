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

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.ExternalReferenceResourceAssembler;
import uk.ac.ebi.biosamples.service.ExternalReferenceService;
import uk.ac.ebi.biosamples.service.SamplePageService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleReadService;

@RestController
@ExposesResourceFor(ExternalReference.class)
@RequestMapping("/externalreferences")
public class ExternalReferenceRestController {

	private final SampleReadService sampleService;
	private final SamplePageService samplePageService;
	private final ExternalReferenceService externalReferenceService;
	
	private final SampleResourceAssembler sampleResourceAssembler;
	private final ExternalReferenceResourceAssembler externalReferenceResourceAssembler;

	private final EntityLinks entityLinks;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public ExternalReferenceRestController(SampleReadService sampleService, 
			SamplePageService samplePageService,
			ExternalReferenceService externalReferenceService,
			SampleResourceAssembler sampleResourceAssembler,
			ExternalReferenceResourceAssembler externalReferenceResourceAssembler,
			EntityLinks entityLinks) {
		this.sampleService = sampleService;
		this.samplePageService = samplePageService;
		this.entityLinks = entityLinks;
		this.sampleResourceAssembler = sampleResourceAssembler;
		this.externalReferenceService = externalReferenceService;
		this.externalReferenceResourceAssembler = externalReferenceResourceAssembler;
	}
	
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<PagedResources<Resource<ExternalReference>>> getPagedHal(
			Pageable page,
			PagedResourcesAssembler<ExternalReference> pageAssembler) {
    	Page<ExternalReference> pageExternalReference = externalReferenceService.getPage(page);
		PagedResources<Resource<ExternalReference>> pagedResources = pageAssembler.toResource(pageExternalReference, 
				externalReferenceResourceAssembler,
				entityLinks.linkToCollectionResource(ExternalReference.class));
		
		return ResponseEntity.ok()
				.body(pagedResources);
    }
	
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value="/{urlhash}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Resource<ExternalReference>> getExternalReferenceHal(@PathVariable String urlhash) {
    	ExternalReference externalReference = externalReferenceService.getExternalReference(urlhash);
    	if (externalReference == null) {
    		return ResponseEntity.notFound().build();
    	}
    	Resource<ExternalReference> resource = externalReferenceResourceAssembler.toResource(externalReference);
		return ResponseEntity.ok()
				.body(resource);
    }

	
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value="/{urlhash}/samples", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<PagedResources<Resource<Sample>>> getExternalReferenceSamplesHal(@PathVariable String urlhash,
			Pageable pageable,
			PagedResourcesAssembler<Sample> pageAssembler) {

    	//get the response as if we'd called the externalReference endpoint
    	ResponseEntity<Resource<ExternalReference>> externalReferenceResponse = getExternalReferenceHal(urlhash);
    	if (!externalReferenceResponse.getStatusCode().is2xxSuccessful()) {
    		//propagate any non-2xx status code from /{id}/ to this endpoint
    		return ResponseEntity.status(externalReferenceResponse.getStatusCode()).build();
    	}

    	//get the content from the services
    	Page<Sample> pageSample = samplePageService.getSamplesOfExternalReference(urlhash, pageable);    	
    	
    	//use the resource assembler and a link to this method to build out the response content
		PagedResources<Resource<Sample>> pagedResources = pageAssembler.toResource(pageSample, sampleResourceAssembler,
			ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(ExternalReferenceRestController.class)
					.getExternalReferenceSamplesHal(urlhash, pageable, pageAssembler))
				.withRel("samples"));
		
		return ResponseEntity.ok()
				.body(pagedResources);
    	
    }
    
}
