package uk.ac.ebi.biosamples.service.upload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;

@Service
public class JsonSchemaStoreAccessibilityCheckService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RestTemplate restTemplate;
    private final BioSamplesProperties bioSamplesProperties;

    public JsonSchemaStoreAccessibilityCheckService(RestTemplate restTemplate, BioSamplesProperties bioSamplesProperties) {
        this.restTemplate = restTemplate;
        this.bioSamplesProperties = bioSamplesProperties;
    }

    public boolean checkJsonSchemaStoreConnectivity() {
        ResponseEntity<String> response = restTemplate.getForEntity(bioSamplesProperties.getSchemaStore(), String.class);

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            log.info("200 received from schema validator");
            return true;
        } else {
            return false;
        }
    }
}
