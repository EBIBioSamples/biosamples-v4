package uk.ac.ebi.biosamples.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
@RequestMapping(value = "/samples", produces = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE,
		MediaType.APPLICATION_XML_VALUE })
@ExposesResourceFor(Sample.class)
public class SampleRestController {

	private SampleService sampleService;

	private SampleResourceAssembler sampleResourceAssembler;

	private DateTimeFormatter lastModifiedFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'");

	public SampleRestController(@Autowired SampleService sampleService,
			@Autowired SampleResourceAssembler sampleResourceAssembler) {
		this.sampleService = sampleService;
		this.sampleResourceAssembler = sampleResourceAssembler;

	}

	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(getClass());

	@CrossOrigin
	@RequestMapping(method = RequestMethod.GET, value = "", produces = { MediaType.APPLICATION_JSON_VALUE,
			MediaTypes.HAL_JSON_VALUE })
	public ResponseEntity<PagedResources<Resource<Sample>>> readAll(Pageable pageable,
			PagedResourcesAssembler<Sample> assembler) {

		Page<Sample> pageSample = sampleService.fetchFindAll(pageable);
		PagedResources<Resource<Sample>> pagedResources = assembler.toResource(pageSample, sampleResourceAssembler);
		pagedResources.add(ControllerLinkBuilder
				.linkTo(ControllerLinkBuilder.methodOn(SampleRestController.class).search()).withRel("search"));
		return ResponseEntity.ok(pagedResources);
	}

	@CrossOrigin
	@RequestMapping(method = RequestMethod.GET, value = "search", produces = { MediaType.APPLICATION_JSON_VALUE,
			MediaTypes.HAL_JSON_VALUE })
	public ResponseEntity<ResourceSupport> search() {
		ResourceSupport resource = new ResourceSupport();
		resource.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SampleRestController.class).search())
				.withSelfRel());
		resource.add(ControllerLinkBuilder
				.linkTo(ControllerLinkBuilder.methodOn(SampleRestController.class).findByText("text", null, null))
				.withRel("findByText"));
		return ResponseEntity.ok(resource);
	}

	@CrossOrigin
	@RequestMapping(method = RequestMethod.GET, value = "search/findByText", produces = {
			MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE })
	public ResponseEntity<PagedResources<Resource<Sample>>> findByText(@RequestParam("text") String text,
			Pageable pageable, PagedResourcesAssembler<Sample> assembler) {

		Page<Sample> pageSample = sampleService.fetchFindByText(text, pageable);
		PagedResources<Resource<Sample>> pagedResources = assembler.toResource(pageSample, sampleResourceAssembler);
		pagedResources.add(ControllerLinkBuilder
				.linkTo(ControllerLinkBuilder.methodOn(SampleRestController.class).search()).withRel("search"));
		return ResponseEntity.ok(pagedResources);
	}

	@CrossOrigin
	@RequestMapping(method = RequestMethod.GET, value = "{accession}", produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<Sample> readXml(@PathVariable String accession) {

		// convert it into the format to return
		Sample sample = null;
		try {
			sample = sampleService.fetch(accession);
		} catch (IllegalArgumentException e) {
			// did not exist, throw 404
			return ResponseEntity.notFound().build();
		}
		
		// check if the release date is in the future and if so return it as private
		if (sample != null && LocalDateTime.now().isBefore(sample.getRelease())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		
		
		// create the response object with the appropriate status
		return ResponseEntity.ok()
				.lastModified(sample.getUpdate().toEpochSecond(ZoneOffset.UTC))
				.eTag(String.valueOf(sample.hashCode()))
				.contentType(MediaType.APPLICATION_XML).body(sample);
	}

	@CrossOrigin
	@RequestMapping(method = RequestMethod.GET, value = "{accession}", produces = { MediaType.APPLICATION_JSON_VALUE,
			MediaTypes.HAL_JSON_VALUE })
	public ResponseEntity<Resource<Sample>> readResource(@PathVariable String accession) {
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

		// create the response object with the appropriate status
		return ResponseEntity.ok()
				.lastModified(sample.getUpdate().toEpochSecond(ZoneOffset.UTC))
				.eTag(String.valueOf(sample.hashCode()))
				.contentType(MediaTypes.HAL_JSON).body(sampleResource);
	}

	@RequestMapping(method = RequestMethod.PUT, value = "{accession}", consumes = { MediaType.APPLICATION_JSON_VALUE,
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

	@RequestMapping(method = RequestMethod.POST, consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	public ResponseEntity<Resource<Sample>> submit(@RequestBody Sample sample) {
		log.info("Recieved POST");
		sample = sampleService.store(sample);
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);
		
		// create the response object with the appropriate status
		return ResponseEntity.created(URI.create(sampleResource.getLink("self").getHref())).body(sampleResource);
	}
}
