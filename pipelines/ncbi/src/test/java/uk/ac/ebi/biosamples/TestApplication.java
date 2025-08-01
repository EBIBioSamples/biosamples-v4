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
package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.service.WebinAuthClientService;
import uk.ac.ebi.biosamples.client.utils.ClientProperties;
import uk.ac.ebi.biosamples.core.service.SampleValidator;
import uk.ac.ebi.biosamples.ncbi.MockBioSamplesClient;

@Configuration
public class TestApplication {
  @Bean
  public BioSamplesClient bioSamplesClient(
      ClientProperties clientProperties,
      RestTemplateBuilder restTemplateBuilder,
      SampleValidator sampleValidator,
      ObjectMapper objectMapper) {
    return new MockBioSamplesClient(
        clientProperties.getBiosamplesClientUri(),
        clientProperties.getBiosamplesClientUriV2(),
        restTemplateBuilder,
        sampleValidator,
        webinAuthClientService(),
        clientProperties,
        objectMapper);
  }

  @Bean
  WebinAuthClientService webinAuthClientService() {
    return null;
  }
}
