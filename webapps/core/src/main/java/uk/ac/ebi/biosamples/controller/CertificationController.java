package uk.ac.ebi.biosamples.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.model.certification.RecorderResult;
import uk.ac.ebi.biosamples.service.certification.Pipeline;

@RestController
public class CertificationController {

    @Autowired
    private Pipeline pipeline;

    @PostMapping("/certify")
    public RecorderResult certify(@RequestBody String sampleJson) {
        return pipeline.run(sampleJson);
    }

}
