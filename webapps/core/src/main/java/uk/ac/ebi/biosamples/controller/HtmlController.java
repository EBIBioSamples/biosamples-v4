package uk.ac.ebi.biosamples.controller;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import uk.ac.ebi.biosamples.model.Sample;
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
public class HtmlController {

	private SampleService sampleService;	
	
	public HtmlController(@Autowired SampleService sampleService) {
		this.sampleService = sampleService;
	}
	
	@RequestMapping(value = "/", method = RequestMethod.GET)
    public String index() {		
        return "index";
    }
	
	@RequestMapping(value = "/samples/{accession}", method = RequestMethod.GET)
    public String samplesAccession(Model model, @PathVariable String accession, HttpServletRequest request, HttpServletResponse response) {
		Sample sample = null;
		try {
		    sample = sampleService.fetch(accession);
		} catch (IllegalArgumentException e) {
			//did not exist, throw 404
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return "error/404";
		}
		
		if (sample == null) {
			//throw internal server error
			throw new RuntimeException("Unable to retrieve "+accession);
		}

		// check if the release date is in the future and if so return it as private
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
