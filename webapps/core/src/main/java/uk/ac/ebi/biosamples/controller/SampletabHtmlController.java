package uk.ac.ebi.biosamples.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.biosamples.BioSamplesProperties;

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

    @RequestMapping("/file_submit")
    public String something() {
        return "hello";
    }
}
