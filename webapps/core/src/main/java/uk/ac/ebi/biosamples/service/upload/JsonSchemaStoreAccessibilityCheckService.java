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

  public JsonSchemaStoreAccessibilityCheckService(
      RestTemplate restTemplate, BioSamplesProperties bioSamplesProperties) {
    this.restTemplate = restTemplate;
    this.bioSamplesProperties = bioSamplesProperties;
  }

  public boolean checkJsonSchemaStoreConnectivity() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(bioSamplesProperties.getSchemaStore(), String.class);

    if (response.getStatusCode().equals(HttpStatus.OK)) {
      log.info("200 received from schema validator");
      return true;
    } else {
      return false;
    }
  }
}
