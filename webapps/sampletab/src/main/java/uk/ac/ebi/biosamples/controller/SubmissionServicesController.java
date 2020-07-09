package uk.ac.ebi.biosamples.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.biosamples.BioSamplesProperties;

@Controller
public class SubmissionServicesController {
    private final Logger log = LoggerFactory.getLogger(getClass());

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
        log.warn("ACCESSING DEPRECATED API at SubmissionServicesController /");
        return "index";
    }

    @RequestMapping("/submission")
    public String submissionService() {
        log.warn("ACCESSING DEPRECATED API at SubmissionServicesController /submission");
        return "submission";
    }

    @RequestMapping("/validation")
    public String validationService() {
        log.warn("ACCESSING DEPRECATED API at SubmissionServicesController /validation");
        return "validation";
    }

    @RequestMapping("/accession")
    public String accessionService() {
        log.warn("ACCESSING DEPRECATED API at SubmissionServicesController /accession");
        return "accession";
    }

    @RequestMapping("/file_submit")
    public String something() {
        log.warn("ACCESSING DEPRECATED API at SubmissionServicesController /file_submit");
        return "hello";
    }


}
