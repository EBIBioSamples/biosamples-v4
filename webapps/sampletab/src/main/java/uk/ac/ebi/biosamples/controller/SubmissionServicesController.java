package uk.ac.ebi.biosamples.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SubmissionServicesController {

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
