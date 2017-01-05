package uk.ac.ebi.biosamples.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import uk.ac.ebi.biosamples.models.Sample;
import uk.ac.ebi.biosamples.service.SampleService;

@Controller
@RequestMapping(value = "/samples", produces = MediaType.TEXT_HTML_VALUE)
public class SampleHtmlController {

	@Autowired
	private SampleService sampleService;
	
	@RequestMapping(value = "{accession}", method = RequestMethod.GET)
    public String greeting(Model model, @PathVariable String accession, HttpServletRequest request) {
        Sample sample = sampleService.fetch(accession);        
		model.addAttribute("sample", sample);		
        return "sample";
    }
}
