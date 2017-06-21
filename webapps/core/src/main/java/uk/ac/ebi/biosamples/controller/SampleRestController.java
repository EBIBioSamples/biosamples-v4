package uk.ac.ebi.biosamples.controller;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.service.SamplePageService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;

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
	private final BioSamplesAapService bioSamplesAapService;

	private final SampleResourceAssembler sampleResourceAssembler;

	private final EntityLinks entityLinks;
		
	private Logger log = LoggerFactory.getLogger(getClass());

	public SampleRestController(SampleService sampleService, 
			SamplePageService samplePageService,FilterService filterService, 
			BioSamplesAapService bioSamplesAapService,
			SampleResourceAssembler sampleResourceAssembler, EntityLinks entityLinks) {
		this.sampleService = sampleService;
		this.samplePageService = samplePageService;
		this.bioSamplesAapService = bioSamplesAapService;
		this.filterService = filterService;
		this.sampleResourceAssembler = sampleResourceAssembler;
		this.entityLinks = entityLinks;
	}

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

	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such Sample") // 404
	public class SampleNotFoundException extends RuntimeException {
	}

	@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Sample not accessible") // 403
	public class SampleNotAccessibleException extends RuntimeException {
	}

	@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Sample accession must match URL accession") // 400
	public class SampleAccessionMismatchException extends RuntimeException {
	}

	@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Sample must specify a domain") // 400
	public class SampleDomainMissingException extends RuntimeException {
	}
	
	@PreAuthorize("isAuthenticated()")
	@PutMapping(value = "/{accession}", consumes = { MediaType.APPLICATION_JSON_VALUE })
	public Resource<Sample> put(@PathVariable String accession, 
			@RequestBody Sample sample) {
		
		if (!sample.getAccession().equals(accession)) {
			// if the accession in the body is different to the accession in the
			// url, throw an error
			// TODO create proper exception with right http error code
			throw new SampleAccessionMismatchException();
		}		
		if (sample.getDomain() == null || sample.getDomain().length() == 0) {
			// if the accession in the body is different to the accession in the
			// url, throw an error
			// TODO create proper exception with right http error code
			throw new SampleDomainMissingException();
		}
		log.info("Recieved PUT for " + accession);
		
		//check sample is assigned to a domain that the authenticated user has access to
		if (!bioSamplesAapService.getDomains().contains(sample.getDomain())) {
			throw new SampleNotAccessibleException();
		}
				
		sample = sampleService.store(sample);

		// assemble a resource to return
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);

		// create the response object with the appropriate status
		return sampleResource;
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Resource<Sample>> post(@RequestBody Sample sample) {

		if (sample.getDomain() == null || sample.getDomain().length() == 0) {
			// if the accession in the body is different to the accession in the
			// url, throw an error
			// TODO create proper exception with right http error code
			throw new SampleDomainMissingException();
		}
		log.info("Recieved POST");

		//check sample is assigned to a domain that the authenticated user has access to
		if (!bioSamplesAapService.getDomains().contains(sample.getDomain())) {
			throw new SampleNotAccessibleException();
		}
				
		sample = sampleService.store(sample);
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);

		// create the response object with the appropriate status
		return ResponseEntity.created(URI.create(sampleResource.getLink("self").getHref())).body(sampleResource);
	}

}
