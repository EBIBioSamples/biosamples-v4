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
package uk.ac.ebi.biosamples.service.upload;

import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;

@Service
public class JsonSchemaStoreSchemaRetrievalService {
  private final Logger log = LoggerFactory.getLogger(getClass());
  @Autowired private BioSamplesProperties bioSamplesProperties;

  public Map<String, String> getChecklists() {
    final Map<String, String> schemaAccessions = new TreeMap<>();
    final RestTemplate restTemplate = new RestTemplate();
    final ResponseEntity<String> response;

    restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
    restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

    try {
      final HttpHeaders headers = new HttpHeaders();

      headers.add("Content-Type", "application/json;charset=UTF-8");
      headers.add("Accept", "application/json");

      final HttpEntity<?> entity = new HttpEntity<>(headers);

      log.info("Schema store URL is " + bioSamplesProperties.getSchemaStore());

      response =
          restTemplate.exchange(
              bioSamplesProperties.getSchemaStore() + "/api/v2/schemas/list",
              HttpMethod.GET,
              entity,
              String.class);

    } catch (final Exception ex) {
      log.info("JSON schema store inaccessible", ex);
      throw new RuntimeException("Failed to retrieve schemas from JSON schema store", ex);
    }

    if (response.getStatusCode() != HttpStatus.OK) {
      log.info("JSON schema store inaccessible");
      throw new RuntimeException(
          "Failed to retrieve schemas from JSON schema store. Response: " + response);
    }

    final JSONObject jsonObject = new JSONObject(response.getBody());
    final JSONObject embedded = jsonObject.getJSONObject("_embedded");

    if (embedded != null) {
      final JSONArray schemas = embedded.getJSONArray("schemas");

      if (schemas != null) {
        for (int i = 0; i < schemas.length(); i++) {
          final JSONObject schema = schemas.getJSONObject(i);

          if (schema != null) {
            String schemaName;
            if (schema.get("name") == null) {
              schemaName = schema.getString("accession");
            } else {
              schemaName = schema.getString("name");
            }

            final String schemaAccession = schema.getString("accession");

            schemaAccessions.put(schemaAccession, schemaName + "(" + schemaAccession + ")");
          }
        }
      }
    }

    return schemaAccessions;
  }
}
