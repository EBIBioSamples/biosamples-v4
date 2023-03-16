/*
* Copyright 2021 EMBL - European Bioinformatics Institute
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
import java.io.IOException;
import java.net.URI;
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
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;

@Service
@Qualifier("elixirValidator")
public class ElixirSchemaValidator implements ValidatorI {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final RestTemplate restTemplate;
  private final BioSamplesProperties bioSamplesProperties;
  private final ObjectMapper objectMapper;

  public ElixirSchemaValidator(
      final RestTemplate restTemplate,
      final BioSamplesProperties bioSamplesProperties,
      final ObjectMapper objectMapper) {
    this.restTemplate = restTemplate;
    this.bioSamplesProperties = bioSamplesProperties;
    this.objectMapper = objectMapper;
  }

  @Override
  public void validate(final String schemaId, final String sample) throws IOException {
    final JsonNode sampleJson = objectMapper.readTree(sample);
    final JsonSchema schema = getSchema(schemaId);

    final ValidationRequest validationRequest =
        new ValidationRequest(schema.getSchema(), sampleJson);
    final URI validatorUri = URI.create(bioSamplesProperties.getSchemaValidator());
    final RequestEntity<ValidationRequest> requestEntity =
        RequestEntity.post(validatorUri)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(validationRequest);
    final JsonNode validationResponse =
        restTemplate.exchange(requestEntity, JsonNode.class).getBody();

    if (validationResponse.get("validationState").asText().equalsIgnoreCase("INVALID")) {
      throw new GlobalExceptions.SampleValidationException(
          "Sample validation failed: " + validationResponse.get("validationErrors").toString());
    }
  }

  @Override
  public String validateById(final String schemaAccession, final String sample)
      throws IOException, GlobalExceptions.SchemaValidationException {
    final JsonNode sampleJson = objectMapper.readTree(sample);
    final JsonNode schema = getSchemaByAccession(schemaAccession);

    final ValidationRequest validationRequest = new ValidationRequest(schema, sampleJson);
    final URI validatorUri = URI.create(bioSamplesProperties.getSchemaValidator());
    final RequestEntity<ValidationRequest> requestEntity =
        RequestEntity.post(validatorUri)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(validationRequest);
    final JsonNode validationResponse =
        restTemplate.exchange(requestEntity, JsonNode.class).getBody();

    if (validationResponse.size() > 0) {
      throw new GlobalExceptions.SampleValidationException(
          "Sample validation failed: " + validationResponse);
    }

    return schemaAccession;
  }

  private JsonSchema getSchema(final String schemaId) {
    final URI schemaStoreUri =
        UriComponentsBuilder.fromUriString(
                bioSamplesProperties.getSchemaStore() + "/api/v2/schemas?id={schemaId}")
            .build()
            .expand(schemaId)
            .encode()
            .toUri();

    final RequestEntity<Void> requestEntity =
        RequestEntity.get(schemaStoreUri).accept(MediaType.APPLICATION_JSON).build();
    final ResponseEntity<JsonSchema> schemaResponse =
        restTemplate.exchange(requestEntity, JsonSchema.class);
    if (schemaResponse.getStatusCode() != HttpStatus.OK) {
      log.error(
          "Failed to retrieve schema from JSON Schema Store: {} {}", schemaId, schemaResponse);
      throw new GlobalExceptions.SampleValidationException(
          "Failed to retrieve schema from JSON Schema Store");
    }

    return schemaResponse.getBody();
  }

  private JsonNode getSchemaByAccession(final String schemaAccession) {
    final URI schemaStoreUri =
        UriComponentsBuilder.fromUriString(
                bioSamplesProperties.getSchemaStore() + "/registry/schemas/{accession}")
            .build()
            .expand(schemaAccession)
            .encode()
            .toUri();

    final RequestEntity<Void> requestEntity =
        RequestEntity.get(schemaStoreUri).accept(MediaType.APPLICATION_JSON).build();
    final ResponseEntity<JsonNode> schemaResponse =
        restTemplate.exchange(requestEntity, JsonNode.class);
    if (schemaResponse.getStatusCode() != HttpStatus.OK) {
      log.error(
          "Failed to retrieve schema from JSON Schema Store: {} {}",
          schemaAccession,
          schemaResponse);
      throw new GlobalExceptions.SampleValidationException(
          "Failed to retrieve schema from JSON Schema Store: " + schemaAccession);
    }

    return schemaResponse.getBody();
  }
}
