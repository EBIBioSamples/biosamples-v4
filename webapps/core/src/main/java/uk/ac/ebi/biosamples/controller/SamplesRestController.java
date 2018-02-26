package uk.ac.ebi.biosamples.controller;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.service.SamplePageService;
import uk.ac.ebi.biosamples.service.SampleResourceAssembler;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;
import utils.LinkUtils;

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
	private final FilterService filterService;
	private final BioSamplesAapService bioSamplesAapService;


	private final SampleResourceAssembler sampleResourceAssembler;

	private Logger log = LoggerFactory.getLogger(getClass());

	public SamplesRestController(
			SamplePageService samplePageService,FilterService filterService,
			BioSamplesAapService bioSamplesAapService,
			SampleResourceAssembler sampleResourceAssembler) {
		this.samplePageService = samplePageService;
		this.filterService = filterService;
		this.bioSamplesAapService = bioSamplesAapService;
		this.sampleResourceAssembler = sampleResourceAssembler;
	}
	
	private String decodeText(String text) {
		if (text != null) {
			try {
				//URLDecoder doesn't work right...
				//text = URLDecoder.decode(text, "UTF-8");
				text = UriUtils.decode(text, "UTF-8");
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
					//URLDecoder doesn't work right...
					//filter[i] = URLDecoder.decode(filter[i], "UTF-8");
					filter[i] = UriUtils.decode(filter[i], "UTF-8");
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
			@RequestParam(name = "cursor", required = false) String cursor,
			@RequestParam(name = "page", required = false) final Integer page,
			@RequestParam(name = "size", required = false) final Integer size, 
			@RequestParam(name = "sort", required = false) final String[] sort, 
			PagedResourcesAssembler<Sample> pageAssembler) {

		
		//Need to decode the %20 and similar from the parameters
		//this is *not* needed for the html controller
		text = decodeText(text);
		filter = decodeFilter(filter);
		String effectiveCursor = decodeText(cursor);
			
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

		if (cursor != null) {

			log.info("This cursor = "+effectiveCursor);
			CursorArrayList<Sample> samples = samplePageService.getSamplesByText(text, filters, 
				domains, cursor, effectiveSize);
			log.info("Next cursor = "+samples.getNextCursorMark());
			
			Resources<Resource<Sample>>  resources = new Resources<>(samples.stream()
				.map(s -> sampleResourceAssembler.toResource(s))
				.collect(Collectors.toList()));

			resources.add(ControllerLinkBuilder.linkTo(
				ControllerLinkBuilder.methodOn(SamplesRestController.class)
					.searchHal(text, filter, effectiveCursor, null, size, null, null))
				.withSelfRel());
			
			
			//only display the next link if there is a next cursor to go to
			if (!decodeText(samples.getNextCursorMark()).equals(effectiveCursor) 
					&& !samples.getNextCursorMark().equals("*")) {
				Link next = ControllerLinkBuilder.linkTo(
					ControllerLinkBuilder.methodOn(SamplesRestController.class)
						.searchHal(text, filter, decodeText(samples.getNextCursorMark()), null, size, null, null))
					.withRel(Link.REL_NEXT);
				
				
				//have to manually strip all templating out because it can't handle encoded content
				next = new Link(next.getHref().replaceAll("\\{.*\\}", ""), Link.REL_NEXT);
				
				resources.add(next);	
			}
			
			return resources;
			
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

			
			PageMetadata pageMetadata = new PageMetadata(pageSample.getNumberOfElements(), 
					pageSample.getNumber(), pageSample.getTotalElements(), pageSample.getTotalPages());
			
			Resources<Resource<Sample>> resources = new PagedResources<>(pageSample.getContent().stream()
					.map(s -> sampleResourceAssembler.toResource(s))
					.collect(Collectors.toList()), pageMetadata);			 

			//self link
			resources.add(LinkUtils.cleanLink(ControllerLinkBuilder
				.linkTo(ControllerLinkBuilder.methodOn(SamplesRestController.class)
					.searchHal(text, filter, null, page, size, sort, null))
				.withSelfRel()));
			
			//if there was a previous page, link to it and the first page
			if (effectivePage > 0) {
				resources.add(LinkUtils.cleanLink(ControllerLinkBuilder
						.linkTo(ControllerLinkBuilder.methodOn(SamplesRestController.class)
							.searchHal(text, filter, null, effectivePage-1, size, sort, null))
						.withRel(Link.REL_PREVIOUS)));
				resources.add(LinkUtils.cleanLink(ControllerLinkBuilder
					.linkTo(ControllerLinkBuilder.methodOn(SamplesRestController.class)
						.searchHal(text, filter, null, 0, size, sort, null))
					.withRel(Link.REL_FIRST)));
			}
			
			//if there is a next page, link to it and the last page
			if (effectivePage < pageSample.getTotalPages()) {
				resources.add(LinkUtils.cleanLink(ControllerLinkBuilder
						.linkTo(ControllerLinkBuilder.methodOn(SamplesRestController.class)
							.searchHal(text, filter, null, effectivePage+1, size, sort, null))
						.withRel(Link.REL_NEXT)));
				resources.add(LinkUtils.cleanLink(ControllerLinkBuilder
					.linkTo(ControllerLinkBuilder.methodOn(SamplesRestController.class)
						.searchHal(text, filter, null, pageSample.getTotalPages(), size, sort, null))
					.withRel(Link.REL_LAST)));
			}
			

//			resources.add(ControllerLinkBuilder.linkTo(
//				ControllerLinkBuilder.methodOn(SamplesRestController.class)
//					.searchHal(text, filter, null, page, size, sort, null))
//				.withSelfRel());
			
			// to generate the HAL template correctly, the parameter name must match
			// the requestparam name
			resources.add(LinkUtils.cleanLink(ControllerLinkBuilder
				.linkTo(ControllerLinkBuilder.methodOn(SamplesRestController.class)
					.searchHal(text, filter, "*", null, size, null, null))
				.withRel("cursor")));
		
			resources.add(LinkUtils.cleanLink(ControllerLinkBuilder
				.linkTo(ControllerLinkBuilder.methodOn(SampleAutocompleteRestController.class)
						.getAutocompleteHal(text, filter, null))
				.withRel("autocomplete")));			
			resources.add(LinkUtils.cleanLink(ControllerLinkBuilder
				.linkTo(ControllerLinkBuilder.methodOn(SampleFacetRestController.class)
					.getFacetsHal(text, filter))
				.withRel("facet")));
			resources.add(ControllerLinkBuilder
				.linkTo(ControllerLinkBuilder.methodOn(SampleRestController.class)
					.getSampleHal(null, false))
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
			
			return resources;
		}
		
		//TODO add search link

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
