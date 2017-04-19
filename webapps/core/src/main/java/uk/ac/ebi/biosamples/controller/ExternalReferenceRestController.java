package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.ExternalReferenceResourceAssembler;
import uk.ac.ebi.biosamples.service.ExternalReferenceService;
import uk.ac.ebi.biosamples.service.SampleService;

@RestController
@ExposesResourceFor(ExternalReference.class)
@RequestMapping("/externalreferences")
public class ExternalReferenceRestController {

	private final SampleService sampleService;
	private final ExternalReferenceService externalReferenceService;
	private final ExternalReferenceResourceAssembler externalReferenceResourceAssembler;

	private final EntityLinks entityLinks;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public ExternalReferenceRestController(SampleService sampleService,
			EntityLinks entityLinks, ExternalReferenceService externalReferenceService,
			ExternalReferenceResourceAssembler externalReferenceResourceAssembler) {
		this.sampleService = sampleService;
		this.entityLinks = entityLinks;
		this.externalReferenceService = externalReferenceService;
		this.externalReferenceResourceAssembler = externalReferenceResourceAssembler;
	}
	
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<PagedResources<Resource<ExternalReference>>> getPagedHal(
			Pageable page,
			PagedResourcesAssembler<ExternalReference> pageAssembler) {
    	Page<ExternalReference> pageExternalReference = externalReferenceService.getPage(page);
		PagedResources<Resource<ExternalReference>> pagedResources = pageAssembler.toResource(pageExternalReference, externalReferenceResourceAssembler,
				entityLinks.linkToCollectionResource(ExternalReference.class));
		
		return ResponseEntity.ok()
				.body(pagedResources);
    }
	
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value="/{id}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Resource<ExternalReference>> getSingle(@PathVariable String id) {
    	ExternalReference externalReference = externalReferenceService.getExternalReference(id);
    	Resource<ExternalReference> resource = externalReferenceResourceAssembler.toResource(externalReference);
		return ResponseEntity.ok()
				.body(resource);
    }
}
