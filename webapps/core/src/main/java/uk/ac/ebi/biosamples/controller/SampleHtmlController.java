package uk.ac.ebi.biosamples.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SampleFacet;
import uk.ac.ebi.biosamples.model.SampleFacetValue;
import uk.ac.ebi.biosamples.service.FacetService;
import uk.ac.ebi.biosamples.service.FilterService;
import uk.ac.ebi.biosamples.service.SampleService;

/**
 * Primary controller for HTML operations.
 * 
 * See {@link SampleRestController} for the equivalent REST controller.
 * 
 * @author faulcon
 *
 */
@Controller
@RequestMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
public class SampleHtmlController {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final SampleService sampleService;
	private final FacetService facetService;
	private final FilterService filterService;

	public SampleHtmlController(SampleService sampleService, FacetService facetService, FilterService filterService) {
		this.sampleService = sampleService;
		this.facetService = facetService;
		this.filterService = filterService;
	}

	@GetMapping(value = "/")
	public String index() {
		return "index";
	}

	@GetMapping(value = "/samples")
	public String samples(Model model, @RequestParam(name="text", required=false) String text,
			@RequestParam(name="filter", required=false) String[] filters,
			@RequestParam(name="start", defaultValue="0") Integer start,
			@RequestParam(name="rows", defaultValue="10") Integer rows,
			HttpServletRequest request, HttpServletResponse response) {

		//force a minimum of 1 result
		if (rows < 1) {
			rows = 1;
		}
		//cap it for our protection
		if (rows > 1000) {
			rows = 1000;
		}
		
		MultiValueMap<String, String> filtersMap = filterService.getFilters(filters);
						
		Pageable pageable = new PageRequest(start/rows, rows);
		Page<Sample> pageSample = sampleService.getSamplesByText(text, filtersMap, pageable);
		//default to getting 10 values from 10 facets
		List<SampleFacet> sampleFacets = facetService.getFacets(text, filtersMap, 10, 10);
		
		//build URLs for the facets depending on if they are enabled or not
		UriComponentsBuilder uriBuilder = ServletUriComponentsBuilder.fromRequest(request);		
		Map<String, String> facetsUri = new HashMap<>();		
		List<String> filtersList = new ArrayList<>();
		if (filters != null) {
			filtersList.addAll(Arrays.asList(filters));
		}
		Collections.sort(filtersList);
		
		//now actually build the URLs for each facet
		URI uri;
		for (SampleFacet sampleFacet : sampleFacets) {
			//check if the filter all is on
			if (filtersList.contains(sampleFacet.getLabel())) {
				uri = getFilterUri(uriBuilder, filtersList, null, sampleFacet.getLabel());
				facetsUri.put(sampleFacet.getLabel(), uri.toString());				
			} else {
				//filter is off, add uri to turn it on
				uri = getFilterUri(uriBuilder, filtersList, sampleFacet.getLabel(), null);
				facetsUri.put(sampleFacet.getLabel(), uri.toString());
			}	
			//check for each facet
			for (SampleFacetValue sampleFacetValue : sampleFacet.getValues()) {
				//check if the filter for this facet is on
				String filter = sampleFacet.getLabel()+":"+sampleFacetValue.label;
				if (filtersList.contains(filter)) {
					//filter is on, add uri to turn it off
					uri = getFilterUri(uriBuilder, filtersList, null, filter);
					facetsUri.put(filter, uri.toString());
				} else {
					//filter is off, add uri to turn it on
					uri = getFilterUri(uriBuilder, filtersList, filter, null);
					facetsUri.put(filter, uri.toString());	
				}
			}
		}
								
		model.addAttribute("text", text);		
		model.addAttribute("start", start);
		model.addAttribute("rows", rows);
		model.addAttribute("page", pageSample);
		model.addAttribute("facets", sampleFacets);
		model.addAttribute("facetsuri", facetsUri);
		model.addAttribute("filters", filtersList);
				
		//TODO add "clear all facets" button
		//TODO title of webpage
		
		return "samples";
	}
	
	private URI getFilterUri(UriComponentsBuilder uriBuilder, List<String> filters, String filterAdd, String filterRemove) {
		List<String> tempFiltersList = new ArrayList<>(filters);
		if (filterAdd != null) {
			tempFiltersList.add(filterAdd);
			// if turning on a facet-all filter, remove facet-value filters for that facet
			// if turning on a facet-value filter, remove facet-all filters for that facet
			if (filterAdd.contains(":")) {
				//remove facet-all filters when adding a specific facet
				tempFiltersList.remove(filterAdd.split(":")[0]);
			} else {
				//remove facet-specific filters when adding a filter-all facet
				Iterator<String> it =tempFiltersList.iterator();
				while (it.hasNext()) {
					if (it.next().startsWith(filterAdd+":")) {
						it.remove();
					}
				}
			}
		}
		if (filterRemove != null) {
			tempFiltersList.remove(filterRemove);
		}
		Collections.sort(tempFiltersList);
		String[] tempFiltersArray = new String[tempFiltersList.size()];
		tempFiltersArray = tempFiltersList.toArray(tempFiltersArray);
		URI uri = uriBuilder.cloneBuilder().replaceQueryParam("filter", (Object[])tempFiltersArray).build().encode().toUri();
		return uri;
	}

	@GetMapping(value = "/samples/{accession}")
	public String samplesAccession(Model model, @PathVariable String accession, HttpServletRequest request,
			HttpServletResponse response) {
		Sample sample = null;
		try {
			sample = sampleService.fetch(accession);
		} catch (IllegalArgumentException e) {
			// did not exist, throw 404
			log.info("Returning a 404 for " + request.getRequestURL());
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return "error/404";
		}

		if (sample == null) {
			// throw internal server error
			throw new RuntimeException("Unable to retrieve " + accession);
		}

		// check if the release date is in the future and if so return it as
		// private
		if (sample != null && (sample.getRelease() == null || LocalDateTime.now().isBefore(sample.getRelease()))) {
			response.setStatus(HttpStatus.FORBIDDEN.value());
			return "error/403";
		}

		response.setHeader(HttpHeaders.LAST_MODIFIED, String.valueOf(sample.getUpdate().toEpochSecond(ZoneOffset.UTC)));
		response.setHeader(HttpHeaders.ETAG, String.valueOf(sample.hashCode()));

		model.addAttribute("sample", sample);
		return "sample";
	}
}
