package uk.ac.ebi.biosamples.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.service.AccessionsService;

import java.util.List;

@RestController
@RequestMapping("/accessions")
public class AccessionsRestController {

    private final AccessionsService accessionsService;

    public AccessionsRestController(AccessionsService accessionsService) {
        this.accessionsService = accessionsService;
    }

    @CrossOrigin(methods = RequestMethod.GET)
    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<String> getAccessions(
            @RequestParam(name = "text", required = false) String text,
            @RequestParam(name = "filter", required = false) String[] filter,
            @RequestParam(name = "page", required = false) final Integer page,
            @RequestParam(name = "size", required = false) final Integer size
    ) {
        return accessionsService.getAccessions(text, filter, page, size);
    }

}
