package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.*;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.*;

@RestController
@ExposesResourceFor(Curation.class)
@RequestMapping("/curations")
public class CurationRestController {

	private final SampleReadService sampleService;
	private final SamplePageService samplePageService;
	private final CurationReadService curationReadService;
	
	private final SampleResourceAssembler sampleResourceAssembler;
	private final CurationResourceAssembler curationResourceAssembler;

	private final EntityLinks entityLinks;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public CurationRestController(SampleReadService sampleService, 
			SamplePageService samplePageService,
			CurationReadService curationService,
			SampleResourceAssembler sampleResourceAssembler,
			CurationResourceAssembler curationResourceAssembler,
			EntityLinks entityLinks) {
		this.sampleService = sampleService;
		this.samplePageService = samplePageService;
		this.entityLinks = entityLinks;
		this.sampleResourceAssembler = sampleResourceAssembler;
		this.curationReadService = curationService;
		this.curationResourceAssembler = curationResourceAssembler;
	}
	
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<PagedResources<Resource<Curation>>> getPagedHal(
			Pageable page,
			PagedResourcesAssembler<Curation> pageAssembler) {
    	Page<Curation> pageExternalReference = curationReadService.getPage(page);
		PagedResources<Resource<Curation>> pagedResources = pageAssembler.toResource(pageExternalReference, 
				curationResourceAssembler,
				entityLinks.linkToCollectionResource(Curation.class));
		
		return ResponseEntity.ok()
				.body(pagedResources);
    }
	
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value="/{hash}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Resource<Curation>> getCurationHal(@PathVariable String hash) {
    	Curation curation = curationReadService.getCuration(hash);
    	if (curation == null) {
    		return ResponseEntity.notFound().build();
    	}
    	Resource<Curation> resource = curationResourceAssembler.toResource(curation);
		return ResponseEntity.ok()
				.body(resource);
    }

	
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value="/{hash}/samples", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<PagedResources<Resource<Sample>>> getCurationSamplesHal(@PathVariable String hash,
			Pageable pageable,
			PagedResourcesAssembler<Sample> pageAssembler) {

    	//get the response as if we'd called the externalReference endpoint
    	ResponseEntity<Resource<Curation>> externalReferenceResponse = getCurationHal(hash);
    	if (!externalReferenceResponse.getStatusCode().is2xxSuccessful()) {
    		//propagate any non-2xx status code from /{id}/ to this endpoint
    		return ResponseEntity.status(externalReferenceResponse.getStatusCode()).build();
    	}

    	//get the content from the services
    	Page<Sample> pageSample = samplePageService.getSamplesOfCuration(hash, pageable);    	
    	
    	//use the resource assembler and a link to this method to build out the response content
		PagedResources<Resource<Sample>> pagedResources = pageAssembler.toResource(pageSample, sampleResourceAssembler,
			ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(CurationRestController.class)
					.getCurationSamplesHal(hash, pageable, pageAssembler))
				.withRel("samples"));
		
		return ResponseEntity.ok()
				.body(pagedResources);
    	
    }
    
}
