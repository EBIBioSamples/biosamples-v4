package uk.ac.ebi.biosamples.service.upload;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;

import java.util.ArrayList;
import java.util.List;

@Service
public class JsonSchemaStoreSchemaRetrievalService {
    @Autowired
    private BioSamplesProperties bioSamplesProperties;

    public List<String> getChecklists() {
        final List<String> schemaAccessions = new ArrayList<>();
        final RestTemplate restTemplate = new RestTemplate();
        final ResponseEntity<String> response;

        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        try {
            final HttpHeaders headers = new HttpHeaders();

            headers.add("Content-Type", "application/json;charset=UTF-8");
            headers.add("Accept", "application/json");

            final HttpEntity<?> entity = new HttpEntity<>(headers);

            response =
                    restTemplate.exchange(
                            bioSamplesProperties.getSchemaStore() + "/api/v2/schemas/list",
                            HttpMethod.GET,
                            entity,
                            String.class);

        } catch (final Exception ex) {
            throw new RuntimeException("Failed to retrieve schemas from JSON schema store", ex);
        }

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to retrieve schemas from JSON schema store. Response: " + response);
        }

        final JSONObject jsonObject = new JSONObject(response.getBody());
        final JSONObject embedded = jsonObject.getJSONObject("_embedded");

        if (embedded != null) {
            final JSONArray schemas = embedded.getJSONArray("schemas");

            if (schemas != null) {
                for (int i = 0; i < schemas.length(); i++) {
                    final JSONObject schema = schemas.getJSONObject(i);

                    if (schema != null) {
                        final String schemaAccession = schema.getString("accession");

                        schemaAccessions.add(schemaAccession);
                    }
                }
            }
        }

        return schemaAccessions;
    }
}
