package uk.ac.ebi.biosamples.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
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
    public String index(HttpServletRequest request) {
        return "index";
    }
	
	@RequestMapping(value = "/samples/{accession}", method = RequestMethod.GET)
    public String greeting(Model model, @PathVariable String accession, HttpServletRequest request) {
		Sample sample = null;
		try {
		    sample = sampleService.fetch(accession);
		} catch (IllegalArgumentException e) {
			//did not exist, throw 404
		}   
		model.addAttribute("sample", sample);		
        return "sample";
    }
}
