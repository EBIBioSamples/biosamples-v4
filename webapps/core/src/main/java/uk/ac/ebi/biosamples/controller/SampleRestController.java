package uk.ac.ebi.biosamples.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mvc.BasicLinkBuilder;
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

import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleService;
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

	@Autowired
	private SampleService sampleService;

	private SampleResourceAssembler sampleResourceAssembler;

	@Autowired
	private EntityLinks entityLinks;
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public SampleRestController(@Autowired SampleService sampleService,
			@Autowired SampleResourceAssembler sampleResourceAssembler) {
		this.sampleService = sampleService;
		this.sampleResourceAssembler = sampleResourceAssembler;
	}

    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE })
	public ResponseEntity<PagedResources<Resource<Sample>>> searchHal(
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
		//add the links to each individual sample on the page
		PagedResources<Resource<Sample>> pagedResources = assembler.toResource(pageSample, sampleResourceAssembler);

		//Links for the entire page
		//this is hacky, but no clear way to do this in spring-hateoas currently
		pagedResources.removeLinks();
		UriTemplate selfUriTemplate = new UriTemplate(entityLinks.linkToCollectionResource(Sample.class).getHref()+"{?text,filter,start,rows}");
		pagedResources.add(new Link(selfUriTemplate.toString(),"self"));
		//TODO first/last/next/prev
		UriTemplate autocompleteUriTemplate = new UriTemplate(entityLinks.linkToCollectionResource(Sample.class).getHref()+"/autocomplete{?text,filter,rows}");
		pagedResources.add(new Link(autocompleteUriTemplate.toString(),"autocomplete"));
		UriTemplate facetsUriTemplate = new UriTemplate(entityLinks.linkToCollectionResource(Sample.class).getHref()+"/facets{?text,filter}");
		pagedResources.add(new Link(facetsUriTemplate.toString(),"facets"));
	
		return ResponseEntity.ok()
				.body(pagedResources);
	}
    
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<List<Sample>> searchJson(
			@RequestParam(name="text", required=false) String text,
			@RequestParam(name="filter", required=false) String[] filters,
			@RequestParam(name="start", defaultValue="0") Integer start,
			@RequestParam(name="rows", defaultValue="10") Integer rows,
			PagedResourcesAssembler<Sample> assembler) {
    	ResponseEntity<PagedResources<Resource<Sample>>> halResponse = searchHal(text,filters,start,rows,assembler);

    	List<Sample> sampleList = new ArrayList<Sample>();
    	halResponse.getBody().getContent().stream().forEach(resource -> sampleList.add(resource.getContent()));    	
		return ResponseEntity.status(halResponse.getStatusCode()).headers(halResponse.getHeaders()).body(sampleList);    	
    }
    
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value = "/{accession}", produces = { MediaTypes.HAL_JSON_VALUE })
	public ResponseEntity<Resource<Sample>> getSampleHal(@PathVariable String accession) {
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
    
    @CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value = "/{accession}", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
	public ResponseEntity<Sample> getSampleJsonXml(@PathVariable String accession) {
		ResponseEntity<Resource<Sample>> halResponse = getSampleHal(accession);
		Sample sample = null;
		if (halResponse.getBody() != null && halResponse.getBody().getContent() != null) {
			sample = halResponse.getBody().getContent();
		}
		return ResponseEntity.status(halResponse.getStatusCode()).headers(halResponse.getHeaders()).body(sample);
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
		return ResponseEntity.accepted().body(sampleResource);
	}

	@PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE,	MediaType.APPLICATION_XML_VALUE })
	public ResponseEntity<Resource<Sample>> postJsonXml(@RequestBody Sample sample) {
		log.info("Recieved POST");
		sample = sampleService.store(sample);
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);
		
		// create the response object with the appropriate status
		return ResponseEntity.created(URI.create(sampleResource.getLink("self").getHref())).body(sampleResource);
	}
}
