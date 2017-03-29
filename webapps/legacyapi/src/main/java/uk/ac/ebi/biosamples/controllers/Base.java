package uk.ac.ebi.biosamples.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@Controller
public class Base {

    @GetMapping(value = "/api", produces = APPLICATION_JSON_UTF8_VALUE)
    public @ResponseBody String root() {
        return null;
    }

}
