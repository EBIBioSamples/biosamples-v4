package uk.ac.ebi.biosamples.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.SampleResource;
import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;



/**
 * Primary controller for REST operations both in JSON and XML and both read and write.
 * 
 * See {@link SampleHtmlController} for the HTML equivalent controller.
 * 
 * @author faulcon
 *
 */
@RestController
@RequestMapping(value = "/samples", 
	produces={MediaType.APPLICATION_JSON_VALUE,MediaTypes.HAL_JSON_VALUE,MediaType.APPLICATION_XML_VALUE})
@ExposesResourceFor(Sample.class)
public class SampleRestController {
	
	private SampleService sampleService;
	
	private SampleResourceAssembler sampleResourceAssembler;
	
	private DateTimeFormatter lastModifiedFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
	
	public SampleRestController(@Autowired SampleService sampleService, @Autowired SampleResourceAssembler sampleResourceAssembler) {
		this.sampleService = sampleService;
		this.sampleResourceAssembler = sampleResourceAssembler;
		
	}
	
	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(getClass());
	
	@CrossOrigin
	@RequestMapping(method = RequestMethod.GET, value = "", produces = {MediaType.APPLICATION_JSON_VALUE,MediaTypes.HAL_JSON_VALUE})
	public ResponseEntity<PagedResources<SampleResource>> readAll(
            Pageable pageable,
            PagedResourcesAssembler<Sample> assembler) {
		
		Page<Sample> pageSample = sampleService.fetchFindAll(pageable);
		PagedResources<SampleResource> pagedResources = assembler.toResource(pageSample, sampleResourceAssembler);
		pagedResources.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SampleRestController.class).search()).withRel("search"));
		return ResponseEntity.ok(pagedResources);
	}
	
	@CrossOrigin
	@RequestMapping(method = RequestMethod.GET, value = "search", produces = {MediaType.APPLICATION_JSON_VALUE,MediaTypes.HAL_JSON_VALUE})
	public ResponseEntity<ResourceSupport> search() {
		ResourceSupport resource = new ResourceSupport();
		resource.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SampleRestController.class).search()).withSelfRel());
		resource.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SampleRestController.class).findByText("text", null, null)).withRel("findByText"));
		return ResponseEntity.ok(resource);
	}
	
	@CrossOrigin
	@RequestMapping(method = RequestMethod.GET, value = "search/findByText", produces = {MediaType.APPLICATION_JSON_VALUE,MediaTypes.HAL_JSON_VALUE})
	public ResponseEntity<PagedResources<SampleResource>> findByText(
			@RequestParam("text") String text,
            Pageable pageable,
            PagedResourcesAssembler<Sample> assembler) {

		Page<Sample> pageSample = sampleService.fetchFindByText(text, pageable);
		PagedResources<SampleResource> pagedResources = assembler.toResource(pageSample, sampleResourceAssembler);
		pagedResources.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SampleRestController.class).search()).withRel("search"));
		return ResponseEntity.ok(pagedResources);
	}


	@CrossOrigin
	@RequestMapping(method = RequestMethod.GET, value = "{accession}", produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<Sample> readXml(@PathVariable String accession) {
		
		//convert it into the format to return
		Sample sample = sampleService.fetch(accession);

		//create some http headers to populate for return
		HttpHeaders headers = new HttpHeaders();

		//add a last modified header based on the samples update date
		addLastModifiedHeader(headers, sample.getUpdate());
		 		
		//create the response object with the appropriate status
		ResponseEntity<Sample> response = new ResponseEntity<>(sample, headers, HttpStatus.OK);
		
		return response;
	}

	@CrossOrigin
	@RequestMapping(method = RequestMethod.GET, value = "{accession}", produces = {MediaType.APPLICATION_JSON_VALUE,MediaTypes.HAL_JSON_VALUE})
	public ResponseEntity<SampleResource> readResource(@PathVariable String accession) {
		
		//convert it into the format to return
		Sample sample = sampleService.fetch(accession);
		SampleResource sampleResource = sampleResourceAssembler.toResource(sample);

		//create some http headers to populate for return
		HttpHeaders headers = new HttpHeaders();

		//add a last modified header based on the samples update date
		addLastModifiedHeader(headers, sample.getUpdate());
		 		
		//create the response object with the appropriate status
		ResponseEntity<SampleResource> response = new ResponseEntity<>(sampleResource, headers, HttpStatus.OK);
		
		return response;
	}

	@RequestMapping(method = RequestMethod.PUT, value = "{accession}", consumes = {MediaType.APPLICATION_JSON_VALUE,MediaType.APPLICATION_XML_VALUE})
	public void update(@PathVariable String accession, @RequestBody Sample sample) {
		if (!sample.getAccession().equals(accession)) {
			//if the accession in the body is different to the accession in the url, throw an error
			//TODO create proper exception with right http error code
			throw new RuntimeException("Accessions must match ("+accession+" vs "+sample.getAccession()+")");
		}
		
		log.info("Recieved PUT for "+accession);		
		sampleService.store(sample);		
	}

	@RequestMapping(method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE,MediaType.APPLICATION_XML_VALUE})
	public Sample submit(@RequestBody Sample sample) {
		log.info("Recieved POST");
		sample = sampleService.store(sample);
		return sample;
	}
	
	private void addLastModifiedHeader(HttpHeaders headers, LocalDateTime lastModified) {
		headers.set(HttpHeaders.LAST_MODIFIED, lastModifiedFormatter.format(lastModified));
	}
}
