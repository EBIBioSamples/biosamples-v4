package uk.ac.ebi.biosamples.controller;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.CurationLinkResourceAssembler;
import uk.ac.ebi.biosamples.service.CurationPersistService;
import uk.ac.ebi.biosamples.service.CurationReadService;

@RestController
@RequestMapping("/samples/{accession}/curationlinks")
public class SampleCurationLinksRestController {

	private final CurationReadService curationReadService;
	private final CurationPersistService curationPersistService;
	private final CurationLinkResourceAssembler curationLinkResourceAssembler;
	private final BioSamplesAapService bioSamplesAapService;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public SampleCurationLinksRestController(CurationReadService curationReadService,
			CurationPersistService curationPersistService,
			CurationLinkResourceAssembler curationLinkResourceAssembler, BioSamplesAapService bioSamplesAapService) {
		this.curationReadService = curationReadService;
		this.curationPersistService = curationPersistService;
		this.curationLinkResourceAssembler = curationLinkResourceAssembler;
		this.bioSamplesAapService = bioSamplesAapService;
	}
    
    @CrossOrigin
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<PagedResources<Resource<CurationLink>>> getCurationLinkPageJson(
			@PathVariable String accession,
			Pageable pageable,
			PagedResourcesAssembler<CurationLink> pageAssembler) {
    	
    	Page<CurationLink> page = curationReadService.getCurationLinksForSample(accession, pageable);
    	
		//add the links to each individual sample on the page
		//also adds links to first/last/next/prev at the same time
		PagedResources<Resource<CurationLink>> pagedResources = pageAssembler.toResource(page, curationLinkResourceAssembler,
			ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SampleCurationLinksRestController.class)
				.getCurationLinkPageJson(accession, pageable, pageAssembler)).withSelfRel());
    	
		return ResponseEntity.ok(pagedResources);    
	}
    

    @CrossOrigin
	@GetMapping(value = "/{id}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<Resource<CurationLink>> getCurationLinkJson(
			@PathVariable String accession,
			@PathVariable String id) {
    	
    	CurationLink curationLink = curationReadService.getCurationLink(id);    	
    	Resource<CurationLink> resource = curationLinkResourceAssembler.toResource(curationLink);
    	
		return ResponseEntity.ok(resource);   
	}

	@PreAuthorize("isAuthenticated()")
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Resource<CurationLink>> createCurationLinkJson(
			@PathVariable String accession,
			@RequestBody CurationLink curationLink) {
		
		log.info("Recieved POST for " + curationLink);		
		
		if (curationLink.getSample() == null) {
			//curationLink has no sample, use the one specified in the URL
		} else if (!curationLink.getSample().equals(accession)) {
			//points to a different sample, this is an error
			throw new SampleNotMatchException();
		}
		
		curationLink = bioSamplesAapService.handleCurationLinkDomain(curationLink);	
    	
		//now actually persist it
    	curationLink = curationPersistService.store(curationLink);
    	Resource<CurationLink> resource = curationLinkResourceAssembler.toResource(curationLink);

		// create the response object with the appropriate status
		return ResponseEntity.created(URI.create(resource.getLink("self").getHref()))
				.body(resource);
    }

	@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Sample must match URL or be omitted") // 400
	public static class SampleNotMatchException extends RuntimeException {
	}
}
