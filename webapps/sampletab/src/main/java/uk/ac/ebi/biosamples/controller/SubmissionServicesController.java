package uk.ac.ebi.biosamples.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.biosamples.BioSamplesProperties;

@Controller
public class SubmissionServicesController {

    private BioSamplesProperties bioSamplesProperties;

    public SubmissionServicesController(BioSamplesProperties properties) {
        this.bioSamplesProperties = properties;
    }

    @ModelAttribute
    public void addCoreLink(Model model) {
        model.addAttribute("coreUrl", bioSamplesProperties.getBiosamplesWebappCoreUri());
    }

    @ModelAttribute
    public void addUsiLink(Model model) {
        model.addAttribute("usiUrl", bioSamplesProperties.getUsiCoreUri());
    }

    @RequestMapping("/")
    public String index(){
        return "index";
    }

    @RequestMapping("/submission")
    public String submissionService() {
        return "submission";
    }

    @RequestMapping("/validation")
    public String validationService() {
        return "validation";
    }

    @RequestMapping("/accession")
    public String accessionService() {
        return "accession";
    }

    @RequestMapping("/file_submit")
    public String something() {
        return "hello";
    }


}
