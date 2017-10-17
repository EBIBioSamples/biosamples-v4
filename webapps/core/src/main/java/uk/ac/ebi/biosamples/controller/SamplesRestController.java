package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.*;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filters.Filter;
import uk.ac.ebi.biosamples.service.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

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

	private final SampleReadService sampleService;
	private final SamplePageService samplePageService;
	private final FilterService filterService;
	private final BioSamplesAapService bioSamplesAapService;


	private final SampleResourceAssembler sampleResourceAssembler;

	private final EntityLinks entityLinks;

	private Logger log = LoggerFactory.getLogger(getClass());

	public SamplesRestController(SampleReadService sampleService,
			SamplePageService samplePageService,FilterService filterService,
			BioSamplesAapService bioSamplesAapService,
			SampleResourceAssembler sampleResourceAssembler, EntityLinks entityLinks) {
		this.sampleService = sampleService;
		this.samplePageService = samplePageService;
		this.filterService = filterService;
		this.bioSamplesAapService = bioSamplesAapService;
		this.sampleResourceAssembler = sampleResourceAssembler;
		this.entityLinks = entityLinks;
	}

	@CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<PagedResources<Resource<Sample>>> searchHal(
			@RequestParam(name = "text", required = false) String text,
			@RequestParam(name = "filter", required = false) String[] filter, Pageable page,
			PagedResourcesAssembler<Sample> pageAssembler) {

		
		//Need to decode the %20 and similar from the parameters
		//this is *not* needed for the html controller
		if (text != null) {
			try {
				text = URLDecoder.decode(text, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}		
		if (filter != null) {
			for (int i = 0; i < filter.length; i++) {
				try {
					filter[i] = URLDecoder.decode(filter[i], "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		
		Collection<Filter> filters = filterService.getFiltersCollection(filter);
		Collection<String> domains = bioSamplesAapService.getDomains();

		Page<Sample> pageSample = samplePageService.getSamplesByText(text, filters, domains, page);
		// add the links to each individual sample on the page
		// also adds links to first/last/next/prev at the same time
		PagedResources<Resource<Sample>> pagedResources = pageAssembler.toResource(pageSample, sampleResourceAssembler,
				entityLinks.linkToCollectionResource(Sample.class));

		// to generate the HAL template correctly, the parameter name must match
		// the requestparam name
		pagedResources
				.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SampleAutocompleteRestController.class)
						.getAutocompleteHal(text, filter, null)).withRel("autocomplete"));
		pagedResources.add(ControllerLinkBuilder
				.linkTo(ControllerLinkBuilder.methodOn(SampleFacetRestController.class).getFacetsHal(text, filter))
				.withRel("facet"));
		pagedResources.add(ControllerLinkBuilder
				.linkTo(ControllerLinkBuilder.methodOn(SampleRestController.class).getSampleHal(null))
				.withRel("sample"));

		return ResponseEntity.ok()
				.header(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(1, TimeUnit.MINUTES).cachePublic().getHeaderValue())
				.body(pagedResources);
	}

}
