package uk.ac.ebi.biosamples.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;

@Controller
public class SampleController {

	
	@Autowired
	private MongoSampleRepository mongoSampleRepository;
	
	@RequestMapping(value = "samples/{accession}", produces = MediaType.TEXT_HTML_VALUE, method = RequestMethod.GET)
    public String greeting(Model model, @PathVariable String accession, HttpServletRequest request) {
        
		model.addAttribute("sample", mongoSampleRepository.findOneByAccession(accession));
		
        return "sample";
    }
}
