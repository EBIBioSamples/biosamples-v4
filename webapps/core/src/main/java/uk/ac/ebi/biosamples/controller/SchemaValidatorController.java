package uk.ac.ebi.biosamples.controller;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.service.SchemaValidatorService;

@RestController
public class SchemaValidatorController {

    private final SchemaValidatorService schemaValidatorService;

    public SchemaValidatorController(SchemaValidatorService schemaValidatorService) {
        this.schemaValidatorService = schemaValidatorService;
    }

    @PostMapping(value = "/validation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> validate(@RequestBody String content){
        return schemaValidatorService.validate(content);
    }

}
