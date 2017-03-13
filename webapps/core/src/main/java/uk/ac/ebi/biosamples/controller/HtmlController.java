package uk.ac.ebi.biosamples.controller;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.SolrResultPage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.solr.model.SampleFacets;

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
public class HtmlController {

	private Logger log = LoggerFactory.getLogger(getClass());

	private SampleService sampleService;

	public HtmlController(@Autowired SampleService sampleService) {
		this.sampleService = sampleService;
	}

	@GetMapping(value = "/")
	public String index() {
		return "index";
	}

	@GetMapping(value = "/samples")
	public String samples(Model model, @RequestParam(name="searchTerm", defaultValue="*:*", required=false) String searchTerm,
			@RequestParam(value = "start", defaultValue = "0", required=false) int start,
			@RequestParam(value = "rows", defaultValue = "10", required=false) int rows,
			@RequestParam(value = "filters[]", defaultValue = "", required = false) String[] filters,
			HttpServletRequest request, HttpServletResponse response) {
		
		model.addAttribute("searchTerm", searchTerm);
		
		Pageable pageable = new PageRequest(start/rows, rows);
		Page<Sample> pageSample = sampleService.getSamplesByText(searchTerm, pageable);
		SampleFacets sampleFacets = sampleService.getFacetsByText(searchTerm);
		
		model.addAttribute("page", pageSample);
		model.addAttribute("facets", sampleFacets);	
		model.addAttribute("start", start);
		model.addAttribute("rows", rows);
		
		return "samples";
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
		if (sample != null && LocalDateTime.now().isBefore(sample.getRelease())) {
			response.setStatus(HttpStatus.FORBIDDEN.value());
			return "error/403";
		}

		response.setHeader(HttpHeaders.LAST_MODIFIED, String.valueOf(sample.getUpdate().toEpochSecond(ZoneOffset.UTC)));
		response.setHeader(HttpHeaders.ETAG, String.valueOf(sample.hashCode()));

		model.addAttribute("sample", sample);
		return "sample";
	}
}
