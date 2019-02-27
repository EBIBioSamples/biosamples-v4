package uk.ac.ebi.biosamples.client.service;

import org.springframework.web.client.RestOperations;
import uk.ac.ebi.biosamples.model.Sample;

public class CertificationService {

    private final RestOperations restOperations;

    public CertificationService(RestOperations restOperations) {
        this.restOperations = restOperations;
    }

    public void submit(Sample sample) {
        restOperations.postForLocation("http://localhost:9010/interrogate", sample);
    }
}
