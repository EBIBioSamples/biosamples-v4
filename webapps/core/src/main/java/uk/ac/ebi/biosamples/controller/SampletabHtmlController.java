package uk.ac.ebi.biosamples.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/sampletab")
public class SampletabHtmlController {

    @RequestMapping({"", "/"})
    public String index(){
        return "sampletab/index";
    }

    @RequestMapping("/submission")
    public String submissionService() {
        return "sampletab/submission";
    }

    @RequestMapping("/validation")
    public String validationService() {
        return "sampletab/validation";
    }
    @RequestMapping("/accession")
    public String accessionService() {
        return "sampletab/accession";
    }

}
