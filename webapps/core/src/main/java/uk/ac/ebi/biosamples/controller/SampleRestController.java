package uk.ac.ebi.biosamples.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;

/**
 * Primary controller for REST operations both in JSON and XML and both read and
 * write.
 * 
 * See {@link HtmlController} for the HTML equivalent controller.
 * 
 * @author faulcon
 *
 */
@RestController
@ExposesResourceFor(Sample.class)
public class SampleRestController {

	private SampleService sampleService;

	private SampleResourceAssembler sampleResourceAssembler;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public SampleRestController(@Autowired SampleService sampleService,
			@Autowired SampleResourceAssembler sampleResourceAssembler) {
		this.sampleService = sampleService;
		this.sampleResourceAssembler = sampleResourceAssembler;

	}

    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value = "/samples", produces = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE })
	public ResponseEntity<PagedResources<Resource<Sample>>> search(
			@RequestParam(name="text", required=false) String text,
			@RequestParam(name="filter", required=false) String[] filters,
			@RequestParam(name="start", defaultValue="0") Integer start,
			@RequestParam(name="rows", defaultValue="10") Integer rows,
			PagedResourcesAssembler<Sample> assembler) {

		//force a minimum of 1 result
		if (rows < 1) {
			rows = 1;
		}
		//cap it for our protection
		if (rows > 1000) {
			rows = 1000;
		}
		Pageable pageable = new PageRequest(start/rows, rows);

		MultiValueMap<String, String> filtersMap = sampleService.getFilters(filters);
		Page<Sample> pageSample = sampleService.getSamplesByText(text, filtersMap, pageable);
		PagedResources<Resource<Sample>> pagedResources = assembler.toResource(pageSample, sampleResourceAssembler);
		
		//this is hacky, but no clear way to do this in spring-hateoas currently
		//pagedResources.removeLinks();
		//String linkUri = (BasicLinkBuilder.linkToCurrentMapping().slash("samples").toUri().toString())+"{?text,start,rows,filter}";
		//pagedResources.add(new Link(linkUri, "self"));
		
		//TODO first/last/next/prev
		//pagedResources.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SampleRestController.class).search(text, filters, start, rows, assembler)).withSelfRel());
	
		return ResponseEntity.ok()
				.body(pagedResources);
	}
    
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value = "/samples/autocomplete", produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Autocomplete> autocomplete(
			@RequestParam(name="query", required=false) String text,
			@RequestParam(name="filter", required=false) String[] filters,
			@RequestParam(name="rows", defaultValue="10") Integer rows) {
		MultiValueMap<String, String> filtersMap = sampleService.getFilters(filters);
    	Autocomplete autocomplete = sampleService.getAutocomplete(text, filtersMap, rows);
		return ResponseEntity.ok().body(autocomplete);
	}
    

    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value = "/samples/{accession}", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
	public ResponseEntity<Sample> readResource(@PathVariable String accession) {
		ResponseEntity<Resource<Sample>> halResponse = readResourceHal(accession);
		return ResponseEntity.status(halResponse.getStatusCode()).headers(halResponse.getHeaders()).body(halResponse.getBody().getContent());
	}

    
    
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value = "/samples/{accession}", produces = { MediaTypes.HAL_JSON_VALUE })
	public ResponseEntity<Resource<Sample>> readResourceHal(@PathVariable String accession) {
		log.info("starting call");
		// convert it into the format to return
		Sample sample = null;
		try {
			sample = sampleService.fetch(accession);
		} catch (IllegalArgumentException e) {
			// did not exist, throw 404
			return ResponseEntity.notFound().build();
		}
		
		// check if the release date is in the future and if so return it as private
		if (sample.getRelease().isAfter(LocalDateTime.now())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);
		
		//dummy sleep
//		try {
//			log.info("sleeping");
//			Thread.sleep(1000);
//			log.info("slept");
//		} catch (InterruptedException e) {
//			throw new RuntimeException(e);
//		}
		
		// create the response object with the appropriate status
		ResponseEntity<Resource<Sample>> response =  ResponseEntity.ok()
				.lastModified(sample.getUpdate().toEpochSecond(ZoneOffset.UTC))
				.eTag(String.valueOf(sample.hashCode()))
				.contentType(MediaTypes.HAL_JSON).body(sampleResource);

		log.info("started call");
		return response;
	}

	@PutMapping(value = "/samples/{accession}", consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	public ResponseEntity<Resource<Sample>> update(@PathVariable String accession, @RequestBody Sample sample) {
		if (!sample.getAccession().equals(accession)) {
			// if the accession in the body is different to the accession in the
			// url, throw an error
			// TODO create proper exception with right http error code
			throw new RuntimeException("Accessions must match (" + accession + " vs " + sample.getAccession() + ")");
		}

		// TODO compare to existing version to check if changes

		log.info("Recieved PUT for " + accession);
		sampleService.store(sample);
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);
		
		// create the response object with the appropriate status
		return ResponseEntity.accepted().body(sampleResource);
	}

	@PostMapping(value = "/samples", consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	public ResponseEntity<Resource<Sample>> submit(@RequestBody Sample sample) {
		log.info("Recieved POST");
		sample = sampleService.store(sample);
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);
		
		// create the response object with the appropriate status
		return ResponseEntity.created(URI.create(sampleResource.getLink("self").getHref())).body(sampleResource);
	}
}
