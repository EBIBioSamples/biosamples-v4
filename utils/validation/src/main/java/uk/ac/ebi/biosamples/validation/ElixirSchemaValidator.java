/*
 * Copyright 2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.biosamples.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.exception.SampleValidationException;
import uk.ac.ebi.biosamples.exception.SchemaValidationException;

import java.io.IOException;
import java.net.URI;

@Service
@Qualifier("elixirValidator")
public class ElixirSchemaValidator implements ValidatorI {
    private Logger log = LoggerFactory.getLogger(getClass());

    private final RestTemplate restTemplate;
    private final BioSamplesProperties bioSamplesProperties;
    private final ObjectMapper objectMapper;

    public ElixirSchemaValidator(RestTemplate restTemplate, BioSamplesProperties bioSamplesProperties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.bioSamplesProperties = bioSamplesProperties;
        this.objectMapper = objectMapper;
    }

    public void validate(String schemaId, String sample) throws IOException {
        JsonNode sampleJson = objectMapper.readTree(sample);
        JsonSchema schema = getSchema(schemaId);

        ValidationRequest validationRequest = new ValidationRequest(schema.getSchema(), sampleJson);
        URI validatorUri = URI.create(bioSamplesProperties.getSchemaValidator());
        RequestEntity<ValidationRequest> requestEntity = RequestEntity.post(validatorUri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(validationRequest);
        JsonNode validationResponse = restTemplate.exchange(requestEntity, JsonNode.class).getBody();

        if (validationResponse.get("validationState").asText().equalsIgnoreCase("INVALID")) {
            throw new SampleValidationException("Sample validation failed: " + validationResponse.get("validationErrors").toString());
        }
    }

    public String validateById(String schemaAccession, String sample)
            throws IOException, SchemaValidationException {
        JsonNode sampleJson = objectMapper.readTree(sample);
        JsonNode schema = getSchemaByAccession(schemaAccession);

        ValidationRequest validationRequest = new ValidationRequest(schema, sampleJson);
        URI validatorUri = URI.create(bioSamplesProperties.getSchemaValidator());
        RequestEntity<ValidationRequest> requestEntity = RequestEntity.post(validatorUri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(validationRequest);
        JsonNode validationResponse = restTemplate.exchange(requestEntity, JsonNode.class).getBody();

        if (validationResponse.get("validationState").asText().equalsIgnoreCase("INVALID")) {
            throw new SampleValidationException("Sample validation failed: " + validationResponse.get("validationErrors").toString());
        }

        return schemaAccession;
    }

    public JsonSchema getSchema(String schemaId) {
        URI schemaStoreUri = UriComponentsBuilder
                .fromUriString(bioSamplesProperties.getSchemaStore() + "/api/v2/schemas/id?id={schemaId}")
                .build()
                .expand(schemaId)
                .encode()
                .toUri();

        RequestEntity<Void> requestEntity = RequestEntity.get(schemaStoreUri).accept(MediaType.APPLICATION_JSON).build();
        ResponseEntity<JsonSchema> schemaResponse = restTemplate.exchange(requestEntity, JsonSchema.class);
        if (schemaResponse.getStatusCode() != HttpStatus.OK) {
            log.error("Failed to retrieve schema from JSON Schema Store: {}", schemaId);
            throw new SampleValidationException("Failed to retrieve schema from JSON Schema Store");
        }

        return schemaResponse.getBody();
    }

    public JsonNode getSchemaByAccession(String accession) {
        URI schemaStoreUri = UriComponentsBuilder
                .fromUriString(bioSamplesProperties.getSchemaStore() + "/registry/schemas/{accession}")
                .build()
                .expand(accession)
                .encode()
                .toUri();

        RequestEntity<Void> requestEntity = RequestEntity.get(schemaStoreUri).accept(MediaType.APPLICATION_JSON).build();
        ResponseEntity<JsonNode> schemaResponse = restTemplate.exchange(requestEntity, JsonNode.class);
        if (schemaResponse.getStatusCode() != HttpStatus.OK) {
            log.error("Failed to retrieve schema from JSON Schema Store: {}", accession);
            throw new SampleValidationException("Failed to retrieve schema from JSON Schema Store");
        }

        return schemaResponse.getBody();
    }
}
