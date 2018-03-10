package uk.ac.ebi.biosamples.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/docs")
public class DocumentationController {

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
        return "docs/references/index";
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
