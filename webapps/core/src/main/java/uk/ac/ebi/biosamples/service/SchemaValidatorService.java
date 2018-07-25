package uk.ac.ebi.biosamples.service;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;

@Service
public class SchemaValidatorService {

    private final RestTemplate restTemplate;
    private final BioSamplesProperties bioSamplesProperties;

    public SchemaValidatorService(RestTemplateBuilder restTemplateBuilder, BioSamplesProperties bioSamplesProperties) {
        this.restTemplate = restTemplateBuilder.build();
        this.bioSamplesProperties = bioSamplesProperties;
    }

    public ResponseEntity<String> validate(String objectToValidate) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("accept", MediaType.APPLICATION_JSON_VALUE);

        RequestEntity<String> request = RequestEntity
            .post(this.bioSamplesProperties.getBiosamplesSchemaValidatorService())
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectToValidate);

        return restTemplate.exchange(request, String.class);

    }

}
