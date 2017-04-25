package uk.ac.ebi.biosamples.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

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
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.ExternalReferenceResourceAssembler;
import uk.ac.ebi.biosamples.service.ExternalReferenceService;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;

/**
 * Primary controller for REST operations both in JSON and XML and both read and
 * write.
 * 
 * See {@link SampleHtmlController} for the HTML equivalent controller.
 * 
 * @author faulcon
 *
 */
@RestController
@ExposesResourceFor(Sample.class)
@RequestMapping("/samples")
public class SampleRestController {

	private final SampleService sampleService;
	private final FilterService filterService;
	
	private final SampleResourceAssembler sampleResourceAssembler;
	
	private final EntityLinks entityLinks;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public SampleRestController(SampleService sampleService,
			FilterService filterService,
			SampleResourceAssembler sampleResourceAssembler,
			EntityLinks entityLinks) {
		this.sampleService = sampleService;
		this.filterService = filterService;
		this.sampleResourceAssembler = sampleResourceAssembler;
		this.entityLinks = entityLinks;
	}

    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<PagedResources<Resource<Sample>>> searchHal(
			@RequestParam(name="text", required=false) String text,
			@RequestParam(name="filter", required=false) String[] filter,
			Pageable page,
			PagedResourcesAssembler<Sample> pageAssembler) {

		MultiValueMap<String, String> filtersMap = filterService.getFilters(filter);
		Page<Sample> pageSample = sampleService.getSamplesByText(text, filtersMap, page);
		//add the links to each individual sample on the page
		//also adds links to first/last/next/prev at the same time
		PagedResources<Resource<Sample>> pagedResources = pageAssembler.toResource(pageSample, sampleResourceAssembler,
				entityLinks.linkToCollectionResource(Sample.class));

    	//to generate the HAL template correctly, the parameter name must match the requestparam name
		pagedResources.add(ControllerLinkBuilder.linkTo(
				ControllerLinkBuilder.methodOn(SampleAutocompleteRestController.class)
					.getAutocompleteHal(text, filter, null))
				.withRel("autocomplete"));
		pagedResources.add(ControllerLinkBuilder.linkTo(
				ControllerLinkBuilder.methodOn(SampleFacetRestController.class)
					.getFacetsHal(text, filter))
				.withRel("facet"));
	
		return ResponseEntity.ok(pagedResources);
	}
        
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value = "/{id}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE  })
	public ResponseEntity<Resource<Sample>> getSampleHal(@PathVariable String id) {
		log.info("starting call");
		// convert it into the format to return
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

		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);
		
		// create the response object with the appropriate status
		return ResponseEntity.ok()
				.lastModified(sample.getUpdate().toEpochSecond(ZoneOffset.UTC))
				.eTag(String.valueOf(sample.hashCode()))
				.contentType(MediaTypes.HAL_JSON).body(sampleResource);
	}

	@PutMapping(value = "/{accession}", consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
	public ResponseEntity<Resource<Sample>> putJsonXml(@PathVariable String accession, @RequestBody Sample sample) {
		if (!sample.getAccession().equals(accession)) {
			// if the accession in the body is different to the accession in the
			// url, throw an error
			// TODO create proper exception with right http error code
			throw new RuntimeException("Accessions must match (" + accession + " vs " + sample.getAccession() + ")");
		}

		// TODO compare to existing version to check if changes

		log.info("Recieved PUT for " + accession);
		sampleService.store(sample);
		
		//assemble a resource to return
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);
		
		// create the response object with the appropriate status
		return ResponseEntity.accepted()
				.body(sampleResource);
	}

	@PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE,	MediaType.APPLICATION_XML_VALUE })
	public ResponseEntity<Resource<Sample>> postJsonXml(@RequestBody Sample sample) {
		log.info("Recieved POST");
		sample = sampleService.store(sample);
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);
		
		// create the response object with the appropriate status
		return ResponseEntity.created(URI.create(sampleResource.getLink("self").getHref()))
				.body(sampleResource);
	}
}
