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
            @RequestParam(name = "project", required = false, defaultValue = "") String project,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit
    ) {
        return accessionsService.getAccessions(project, limit);
    }

}
