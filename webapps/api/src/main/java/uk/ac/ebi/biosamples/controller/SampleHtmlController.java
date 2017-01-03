package uk.ac.ebi.biosamples.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.service.InverseRelationshipService;

@Controller
@RequestMapping(value = "/samples", produces = MediaType.TEXT_HTML_VALUE)
public class SampleHtmlController {

	
	@Autowired
	private MongoSampleRepository mongoSampleRepository;
	
	@Autowired
	public InverseRelationshipService inverseRelationshipService;
	
	@RequestMapping(value = "{accession}", method = RequestMethod.GET)
    public String greeting(Model model, @PathVariable String accession, HttpServletRequest request) {
        MongoSample sample = mongoSampleRepository.findOne(accession);

		inverseRelationshipService.addInverseRelationships(sample);
        
		model.addAttribute("sample", sample);		
        return "sample";
    }
}
