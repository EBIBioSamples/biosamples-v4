package uk.ac.ebi.biosamples.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.biosamples.BioSamplesProperties;

@Controller
@RequestMapping("/docs")
public class DocumentationController {

    private BioSamplesProperties bioSamplesProperties;

    public DocumentationController(BioSamplesProperties properties) {
        this.bioSamplesProperties = properties;
    }

    //TODO: Convert this to use ControllerAdvice
    @ModelAttribute
    public void addCoreLink(Model model) {
        model.addAttribute("sampletabUrl", bioSamplesProperties.getBiosamplesWebappSampletabUri());
    }

    @GetMapping
    public String helpIndex() {
        return "docs/index";
    }

    @GetMapping(value = "/{page}")
    public String helpBasePage(@PathVariable String page) {
        return "docs/"+page;
    }

    @GetMapping(value = "/guides/")
    public String helpGuideIndex() {
        return "docs/guides/index";
   }

    @GetMapping(value = "/guides/{page}")
    public String helpGuidePage(@PathVariable String page) {
        return "docs/guides/"+page;
    }

    @GetMapping(value = "/references/")
    public String helpReferenceIndex() {
        return "docs/references/overview";
    }

    @GetMapping(value = "/references/{page}")
    public String helpReferencePage(@PathVariable String page) {
        return "docs/references/" + page;
    }

    @GetMapping(value = "/references/api")
    public String helpApiReferenceHome() {
        return "docs/references/api/overview";
    }

    @GetMapping(value = "/references/api/{page}")
    public String helpApiReferencePage(@PathVariable String page) {
        return "docs/references/api/"+page;
    }
}
