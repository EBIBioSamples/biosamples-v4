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
    public String helpIndex(@PathVariable String page) {
        return "docs/"+page;
    }

    @GetMapping(value = "/api/guides/{page}")
    public String helpApiGuidesPage(@PathVariable String page) {
        return "docs/api/guides/"+page;
    }

    @GetMapping(value = "/api/references/{page}")
    public String helpApiReferencePage(@PathVariable String page) {
        return "docs/api/references/"+page;
    }
}
