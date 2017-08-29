package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.JsonLDSample;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

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
	private final SamplePageService samplePageService;
	private final FilterService filterService;

	private final SampleResourceAssembler sampleResourceAssembler;

	private final EntityLinks entityLinks;

    private final JsonLDService jsonLDService;

    private Logger log = LoggerFactory.getLogger(getClass());

	public SampleRestController(SampleService sampleService, 
			SamplePageService samplePageService,FilterService filterService,
			SampleResourceAssembler sampleResourceAssembler, EntityLinks entityLinks,
            JsonLDService jsonLDService) {
		this.sampleService = sampleService;
		this.samplePageService = samplePageService;
		this.filterService = filterService;
		this.sampleResourceAssembler = sampleResourceAssembler;
		this.jsonLDService = jsonLDService;
		this.entityLinks = entityLinks;
	}

	@CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value = "/{accession}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public Resource<Sample> getSampleHal(@PathVariable String accession) {
		log.trace("starting call");
		// convert it into the format to return
		Optional<Sample> sample = sampleService.fetch(accession);
		if (!sample.isPresent()) {
			throw new SampleNotFoundException();
		}

		// check if the release date is in the future and if so return it as
		// private
		if (sample.get().getRelease().isAfter(LocalDateTime.now())) {
			throw new SampleNotAccessibleException();
		}

		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample.get());

		return sampleResource;
	}

	@CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(value = "/{accession}", produces = { MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE })
	public Sample getSampleXml(@PathVariable String accession) {
		return this.getSampleHal(accession).getContent();
	}

	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such Sample") // 404
	public class SampleNotFoundException extends RuntimeException {
	}

	@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Sample not accessible") // 403
	public class SampleNotAccessibleException extends RuntimeException {
	}


    @CrossOrigin(methods = RequestMethod.GET)
    @GetMapping(value = "/{accession}", produces = "application/ld+json")
    public JsonLDSample getJsonLDSample(@PathVariable String accession) {
		Optional<Sample> sample = sampleService.fetch(accession);
		if (!sample.isPresent()) {
			throw new SampleNotFoundException();
		}

        // check if the release date is in the future and if so return it as
        // private
        if (sample.get().getRelease().isAfter(LocalDateTime.now())) {
			throw new SampleNotAccessibleException();
        }

        JsonLDSample jsonLDSample = jsonLDService.sampleToJsonLD(sample.get());
        
        return jsonLDSample;
    }

	@PutMapping(value = "/{accession}", consumes = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Resource<Sample>> put(@PathVariable String accession, @RequestBody Sample sample) {
		if (!sample.getAccession().equals(accession)) {
			// if the accession in the body is different to the accession in the
			// url, throw an error
			// TODO create proper exception with right http error code
			throw new RuntimeException("Accessions must match (" + accession + " vs " + sample.getAccession() + ")");
		}

		// TODO compare to existing version to check if changes

		log.debug("Recieved PUT for " + accession+" "+sample);
		sample = sampleService.store(sample);

		// assemble a resource to return
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);

		// create the response object with the appropriate status
		return ResponseEntity.accepted().body(sampleResource);
	}

	@PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Resource<Sample>> post(@RequestBody Sample sample) {
		log.debug("Recieved POST for "+sample);
		sample = sampleService.store(sample);
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);

		// create the response object with the appropriate status
		//TODO work out how to avoid using ResponseEntity but also set location header
		return ResponseEntity.created(URI.create(sampleResource.getLink("self").getHref())).body(sampleResource);
	}

}
