package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.*;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.JsonLDSample;
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
import java.util.concurrent.TimeUnit;

import java.net.URI;
import java.time.LocalDateTime;

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
		log.info("starting call");
		// convert it into the format to return
		Sample sample = null;
		try {
			sample = sampleService.fetch(accession);
		} catch (IllegalArgumentException e) {
			// did not exist, throw 404
			//return ResponseEntity.notFound().build();
			throw new SampleNotFoundException();
		}

		if (sample.getName() == null) {
			// if it has no name, then its just created by accessioning or
			// reference
			// can't read it, but could put to it
			// TODO use METHOD_NOT_ALLOWED
			// TODO make sure "options" is correct for this
			throw new SampleNotFoundException();
		}

		// check if the release date is in the future and if so return it as
		// private
		if (sample.getRelease().isAfter(LocalDateTime.now())) {
			throw new SampleNotAccessibleException();
		}

		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);

		// create the response object with the appropriate status
		return sampleResource;
		/*
		return ResponseEntity.ok().lastModified(sample.getUpdate().toInstant(ZoneOffset.UTC).toEpochMilli())
				.header(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(1, TimeUnit.MINUTES).cachePublic().getHeaderValue())
				.eTag(String.valueOf(sample.hashCode())).contentType(MediaTypes.HAL_JSON).body(sampleResource);
		*/
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
    public ResponseEntity<JsonLDSample> getJsonLDSample(@PathVariable String accession) {
        Sample sample = null;
        try {
            sample = sampleService.fetch(accession);
        } catch (IllegalArgumentException e) {
            // did not exist, throw 404
            return ResponseEntity.notFound().build();
        }
        if (sample.getName() == null) {
            // if it has no name, then its just created by accessioning or
            // reference
            // can't read it, but could put to it
            // TODO make sure "options" is correct for this
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        // check if the release date is in the future and if so return it as
        // private
        if (sample.getRelease().isAfter(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        JsonLDSample jsonLDSample = jsonLDService.sampleToJsonLD(sample);

        // create the response object with the appropriate status
        return ResponseEntity.ok().lastModified(sample.getUpdate().toEpochSecond(ZoneOffset.UTC))
                //.header(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(1, TimeUnit.MINUTES).cachePublic().getHeaderValue())
                .eTag(String.valueOf(sample.hashCode())).body(jsonLDSample);
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

		log.info("Recieved PUT for " + accession);
		sample = sampleService.store(sample);

		// assemble a resource to return
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);

		// create the response object with the appropriate status
		return ResponseEntity.accepted().body(sampleResource);
	}

	@PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Resource<Sample>> post(@RequestBody Sample sample) {
		log.info("Recieved POST");
		sample = sampleService.store(sample);
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);

		// create the response object with the appropriate status
		//TODO work out how to avoid using ResponseEntity but also set location header
		return ResponseEntity.created(URI.create(sampleResource.getLink("self").getHref())).body(sampleResource);
	}

}
