package uk.ac.ebi.biosamples.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.core.LinkBuilderSupport;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.filter.FilterType;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.service.SamplePageService;
import uk.ac.ebi.biosamples.service.SampleReadService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;

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
	
	private String decodeText(String text) {
		if (text != null) {
			try {
				text = URLDecoder.decode(text, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}		
		return text;		
	}
	
	private String[] decodeFilter(String[] filter) {
		if (filter != null) {
			for (int i = 0; i < filter.length; i++) {
				try {
					filter[i] = URLDecoder.decode(filter[i], "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return filter;
	}

	@CrossOrigin(methods = RequestMethod.GET)
	@GetMapping(produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public Resources<Resource<Sample>> searchHal(
			@RequestParam(name = "text", required = false) String text,
			@RequestParam(name = "filter", required = false) String[] filter, 
			@RequestParam(name = "cursor", required = false) final String cursor,
			@RequestParam(name = "page", required = false) final Integer page,
			@RequestParam(name = "size", required = false) final Integer size, 
			@RequestParam(name = "sort", required = false) final String[] sort, 
			PagedResourcesAssembler<Sample> pageAssembler) {

		
		//Need to decode the %20 and similar from the parameters
		//this is *not* needed for the html controller
		text = decodeText(text);
		filter = decodeFilter(filter);
			
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
		
		Collection<Filter> filters = filterService.getFiltersCollection(filter);
		Collection<String> domains = bioSamplesAapService.getDomains();

		Resources<Resource<Sample>> resources;
		if (cursor != null) {
			
			CursorArrayList<Sample> samples = samplePageService.getSamplesByText(text, filters, 
				domains, cursor, effectiveSize);
			
			resources = new Resources<>(samples.stream()
				.map(s -> sampleResourceAssembler.toResource(s))
				.collect(Collectors.toList()));

			resources.add(ControllerLinkBuilder.linkTo(
				ControllerLinkBuilder.methodOn(SamplesRestController.class)
					.searchHal(text, filter, cursor, null, size, null, null))
				.withSelfRel());
			
			//only display the next link if there is a next cursor to go to
			if (!samples.getNextCursorMark().equals(cursor)) {
				resources.add(ControllerLinkBuilder.linkTo(
					ControllerLinkBuilder.methodOn(SamplesRestController.class)
						.searchHal(text, filter, samples.getNextCursorMark(), null, size, null, null))
					.withRel(Link.REL_NEXT));	
			}
			
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
			// add the links to each individual sample on the page
			// also adds links to first/last/next/prev at the same time
			resources = pageAssembler.toResource(pageSample, sampleResourceAssembler,
					ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SamplesRestController.class)
							.searchHal(text, filter, null, page, size, sort, null))
						.withSelfRel());
			
			// to generate the HAL template correctly, the parameter name must match
			// the requestparam name
			resources.add(ControllerLinkBuilder
					.linkTo(ControllerLinkBuilder.methodOn(SamplesRestController.class)
							.searchHal(text, filter, "*", null, size, null, null))
					.withRel("cursor"));
			
			resources
					.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SampleAutocompleteRestController.class)
							.getAutocompleteHal(text, filter, null)).withRel("autocomplete"));
			
			resources.add(ControllerLinkBuilder
					.linkTo(ControllerLinkBuilder.methodOn(SampleFacetRestController.class).getFacetsHal(text, filter))
					.withRel("facet"));
			resources.add(ControllerLinkBuilder
					.linkTo(ControllerLinkBuilder.methodOn(SampleRestController.class).getSampleHal(null, false))
					.withRel("sample"));
			
			/*
			if (filters.stream().allMatch(f -> !f.getType().equals(FilterType.DATE_FILTER))) {
	
				String[] templatedFilters = new String[1];
				templatedFilters[0] = FilterType.DATE_FILTER.getSerialization()+":update:from{ISO-8601from}until{ISO-8601until}";
				pagedResources.add(ControllerLinkBuilder
						.linkTo(ControllerLinkBuilder.methodOn(SamplesRestController.class)
								.searchHal(text, templatedFilters, null, null))
						.withRel("samplesbyUpdateDate"));
			}
			*/
		}
		
		//TODO add search link

		return resources;
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
}
