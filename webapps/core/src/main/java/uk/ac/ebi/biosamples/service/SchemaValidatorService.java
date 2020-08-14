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
package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;

@Service
public class SchemaValidatorService {

  Logger log = LoggerFactory.getLogger(getClass());

  private final String noErrorBody = "[]";
  private final RestTemplate restTemplate;
  private final BioSamplesProperties bioSamplesProperties;
  private final ObjectMapper objectMapper;
  private final ApplicationContext applicationContext;

  public SchemaValidatorService(
      RestTemplateBuilder restTemplateBuilder,
      ObjectMapper mapper,
      BioSamplesProperties bioSamplesProperties,
      ApplicationContext applicationContext) {
    this.restTemplate = restTemplateBuilder.build();
    this.objectMapper = mapper;
    this.bioSamplesProperties = bioSamplesProperties;
    this.applicationContext = applicationContext;
  }

  public ResponseEntity<String> validate(String objectToValidate) {
    // TODO reanable this later
    //        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    //        headers.add("accept", MediaType.APPLICATION_JSON_VALUE);
    //
    //        RequestEntity<String> request = RequestEntity
    //            .post(this.bioSamplesProperties.getBiosamplesSchemaValidatorServiceUri())
    //            .contentType(MediaType.APPLICATION_JSON)
    //            .body(objectToValidate);
    //
    //        return restTemplate.exchange(request, String.class);
    return ResponseEntity.ok(noErrorBody);
  }

  public ResponseEntity<String> validate(Object object, URI schema) {
    Resource schemaResource = applicationContext.getResource(schema.toString());

    if (!schemaResource.exists()) {
      log.error("Schema resource for " + schema + " doesn't exists");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Provided schema at " + schema + " doesn't exists");
    }

    try {

      Object jsonSchema =
          this.objectMapper.readValue(schemaResource.getInputStream(), Object.class);

      Map<String, Object> body = new HashMap<>();
      body.put("schema", jsonSchema);
      body.put("object", object);

      return this.validate(this.objectMapper.writeValueAsString(body));

    } catch (IOException e) {
      log.error("Error while reading schema file " + schema);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              "An error occurred while reading the schema at "
                  + schema
                  + ". Please try later or contact biosamples@ebi.ac.uk");
    }
  }
}
