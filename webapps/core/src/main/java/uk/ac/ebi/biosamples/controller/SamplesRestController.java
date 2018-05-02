package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.*;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.*;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;
import uk.ac.ebi.biosamples.utils.LinkUtils;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
public class SamplesRestController {

	private final SamplePageService samplePageService;
	private final SampleService sampleService;
	private final FilterService filterService;
	private final BioSamplesAapService bioSamplesAapService;
	private final SampleManipulationService sampleManipulationService;
	private final BioSamplesProperties bioSamplesProperties;
	private final SampleResourceAssembler sampleResourceAssembler;

	private Logger log = LoggerFactory.getLogger(getClass());

	public SamplesRestController(
			SamplePageService samplePageService, FilterService filterService,
			BioSamplesAapService bioSamplesAapService,
			SampleResourceAssembler sampleResourceAssembler,
			SampleManipulationService sampleManipulationService,
			SampleService sampleService,
			BioSamplesProperties bioSamplesProperties) {
		this.samplePageService = samplePageService;
		this.filterService = filterService;
		this.bioSamplesAapService = bioSamplesAapService;
		this.sampleResourceAssembler = sampleResourceAssembler;
		this.sampleManipulationService = sampleManipulationService;
		this.sampleService = sampleService;
		this.bioSamplesProperties = bioSamplesProperties;
	}

	//must return a ResponseEntity so that cache headers can be set
	@CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Resources<Resource<Sample>>> searchHal(
			@RequestParam(name = "text", required = false) String text,
			@RequestParam(name = "filter", required = false) String[] filter, 
			@RequestParam(name = "cursor", required = false) String cursor,
			@RequestParam(name = "page", required = false) final Integer page,
			@RequestParam(name = "size", required = false) final Integer size, 
			@RequestParam(name = "sort", required = false) final String[] sort, 
			PagedResourcesAssembler<Sample> pageAssembler) {

		
		//Need to decode the %20 and similar from the parameters
		//this is *not* needed for the html controller
		String decodedText = LinkUtils.decodeText(text);
		String[] decodedFilter = LinkUtils.decodeTexts(filter);
		String decodedCursor = LinkUtils.decodeText(cursor);
			
		int effectivePage;
		if (page == null) {
			effectivePage = 0;
		} else {
			effectivePage = page;
		}
		int effectiveSize;
		if (size == null) {
			effectiveSize = 20;
		} else {
			effectiveSize = size;
		}
		
		Collection<Filter> filters = filterService.getFiltersCollection(decodedFilter);
		Collection<String> domains = bioSamplesAapService.getDomains();
		

		//Note - EBI load balancer does cache but doesn't add age header, so clients could cache up to twice this age
		CacheControl cacheControl = CacheControl.maxAge(bioSamplesProperties.getBiosamplesCorePageCacheMaxAge(), TimeUnit.SECONDS);
		//if the user has access to any domains, then mark the response as private as must be using AAP and responses will be different
		if (domains.size() > 0) {
			cacheControl.cachePrivate();
		}

		if (cursor != null) {

			log.trace("This cursor = "+decodedCursor);
			CursorArrayList<Sample> samples = samplePageService.getSamplesByText(decodedText, filters, 
				domains, decodedCursor, effectiveSize);
			log.trace("Next cursor = "+samples.getNextCursorMark());
			
			Resources<Resource<Sample>>  resources = new Resources<>(samples.stream()
					.map(s -> s != null ? sampleResourceAssembler.toResource(s) : null)
				.collect(Collectors.toList()));

			resources.add(getCursorLink(decodedText, decodedFilter, decodedCursor, effectiveSize, Link.REL_SELF));
			//only display the next link if there is a next cursor to go to
			if (!LinkUtils.decodeText(samples.getNextCursorMark()).equals(decodedCursor) 
					&& !samples.getNextCursorMark().equals("*")) {
				resources.add(getCursorLink(decodedText, decodedFilter, samples.getNextCursorMark(), effectiveSize, Link.REL_NEXT));				
			}
			
			//Note - EBI load balancer does cache but doesn't add age header, so clients could cache up to twice this age
			return ResponseEntity.ok()
					.cacheControl(cacheControl)
					.body(resources);
			
		} else {	
			
			String effectiveSort[] = sort;
			if (sort == null) {
				//if there is no existing sort, sort by score then accession
				effectiveSort = new String[2];
				effectiveSort[0] = "score,desc";
				effectiveSort[1] = "id,asc";
			} 
			Sort pageSort = new Sort(Arrays.stream(effectiveSort).map(this::parseSort).collect(Collectors.toList()));
			Pageable pageable = new PageRequest(effectivePage, effectiveSize, pageSort);
			
			Page<Sample> pageSample = samplePageService.getSamplesByText(text, filters, domains, pageable);

			
			PageMetadata pageMetadata = new PageMetadata(effectiveSize,
					pageSample.getNumber(), pageSample.getTotalElements(), pageSample.getTotalPages());
			
			Resources<Resource<Sample>> resources = new PagedResources<>(pageSample.getContent().stream()
					.map(s -> s != null ? sampleResourceAssembler.toResource(s) : null)
					.collect(Collectors.toList()), pageMetadata);			 


			//if theres more than one page, link to first and last
			if (pageSample.getTotalPages() > 1) {
				resources.add(getPageLink(decodedText, decodedFilter, 0, effectiveSize, sort, Link.REL_FIRST));				
			}
			//if there was a previous page, link to it
			if (effectivePage > 0) {
				resources.add(getPageLink(decodedText, decodedFilter, effectivePage-1, effectiveSize, sort, Link.REL_PREVIOUS));
			}
			resources.add(getPageLink(decodedText, decodedFilter, effectivePage, effectiveSize, sort, Link.REL_SELF));
			
			//if there is a next page, link to it 
			if (effectivePage < pageSample.getTotalPages()-1) {
				resources.add(getPageLink(decodedText, decodedFilter, effectivePage+1, effectiveSize, sort, Link.REL_NEXT));
			}
			//if theres more than one page, link to first and last
			if (pageSample.getTotalPages() > 1) {
				resources.add(getPageLink(decodedText, decodedFilter, pageSample.getTotalPages(), effectiveSize, sort, Link.REL_LAST));				
			}

			//if we are on the first page and not sorting
			if (effectivePage==0 && (sort==null || sort.length==0)) {
				resources.add(getCursorLink(decodedText, decodedFilter, "*", effectiveSize, "cursor"));
			}
			
			//if there is no search term, and on first page, add a link to use search
			//TODO
//			if (text.trim().length() == 0 && page == 0) {
//				resources.add(LinkUtils.cleanLink(ControllerLinkBuilder
//					.linkTo(ControllerLinkBuilder.methodOn(SamplesRestController.class)
//						.searchHal(null, filter, null, page, effectiveSize, sort, null))
//					.withRel("search")));
//			}
			
			resources.add(SampleAutocompleteRestController.getLink(decodedText, decodedFilter, null, "autocomplete"));
			
			
			UriComponentsBuilder uriComponentsBuilder = ControllerLinkBuilder.linkTo(SamplesRestController.class).toUriComponentsBuilder();
			//This is a bit of a hack, but best we can do for now...
			resources.add(new Link(uriComponentsBuilder.build(true).toUriString()+"/{accession}", "sample"));
			
			return ResponseEntity.ok()
					.cacheControl(cacheControl)
					.body(resources);
		}
	}
	
	private Order parseSort(String sort) {
		if(sort.endsWith(",desc")) {
			return new Order(Sort.Direction.DESC, sort.substring(0, sort.length()-5));
		} else if(sort.endsWith(",asc")) {
			return new Order(Sort.Direction.ASC, sort.substring(0, sort.length()-4));
		} else {
			return new Order(null, sort);
		}
	}
	
	/**
	 * ControllerLinkBuilder seems to have problems linking to the same controller?
	 * Split out into manual manipulation for greater control
	 * 
	 * @param text
	 * @param filter
	 * @param cursor
	 * @param size
	 * @param rel
	 * @return
	 */
	public static Link getCursorLink(String text, String[] filter, String cursor, int size, String rel) {
		UriComponentsBuilder builder = ControllerLinkBuilder.linkTo(SamplesRestController.class)
				.toUriComponentsBuilder();
		if (text != null && text.trim().length() > 0) {
			builder.queryParam("text", text);
		}
		if (filter != null) {
			for (String filterString : filter) {
				builder.queryParam("filter",filterString);				
			}
		}
		builder.queryParam("cursor", cursor);
		builder.queryParam("size", size);
		return new Link(builder.toUriString(), rel);
	}
	
	public static Link getPageLink(String text, String[] filter, int page, int size, String[] sort, String rel) {
		UriComponentsBuilder builder = ControllerLinkBuilder.linkTo(SamplesRestController.class)
				.toUriComponentsBuilder();
		if (text != null && text.trim().length() > 0) {
			builder.queryParam("text", text);
		}
		if (filter != null) {
			for (String filterString : filter) {
				builder.queryParam("filter",filterString);				
			}
		}
		builder.queryParam("page", page);
		builder.queryParam("size", size);
		if (sort != null) {
			for (String sortString : sort) {
				builder.queryParam("sort",sortString);				
			}
		}
		return new Link(builder.toUriString(), rel);
	}
	


	@PreAuthorize("isAuthenticated()")
	@PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Resource<Sample>> post(@RequestBody Sample sample,
			@RequestParam(name = "setupdatedate", required = false, defaultValue="true") boolean setUpdateDate,
            @RequestParam(name = "setfulldetails", required = false, defaultValue = "false") boolean setFullDetails) {
		
		log.debug("Recieved POST for "+sample);
		if (sample.hasAccession()) {
			throw new SampleWithAccessionSumbissionException();
		}

		sample = bioSamplesAapService.handleSampleDomain(sample);
		
		//TODO disallow previously accessioned samples - BSD-1186

		//TODO disallow previously accessioned samples - BSD-1186

		//limit use of this method to write super-users only
		if (bioSamplesAapService.isWriteSuperUser() && setUpdateDate) {
			sample = Sample.build(sample.getName(), sample.getAccession(), sample.getDomain(), 
					sample.getRelease(), Instant.now(),
					sample.getCharacteristics(), sample.getRelationships(), sample.getExternalReferences(), 
					sample.getOrganizations(), sample.getContacts(), sample.getPublications());
		}

		if (!setFullDetails) {
			sample = sampleManipulationService.removeLegacyFields(sample);
		}
		
		sample = sampleService.store(sample);
		
		// assemble a resource to return
		Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);

		// create the response object with the appropriate status
		//TODO work out how to avoid using ResponseEntity but also set location header
		return ResponseEntity.created(URI.create(sampleResource.getLink("self").getHref())).body(sampleResource);
	}

	@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "New sample submission should not contain an accession") // 400
	public static class SampleWithAccessionSumbissionException extends RuntimeException {
	}

}
