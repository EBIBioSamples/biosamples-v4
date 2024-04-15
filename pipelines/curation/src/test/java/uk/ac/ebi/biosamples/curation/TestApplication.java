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
package uk.ac.ebi.biosamples.curation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.service.AapClientService;
import uk.ac.ebi.biosamples.client.utils.ClientProperties;
import uk.ac.ebi.biosamples.service.CurationApplicationService;
import uk.ac.ebi.biosamples.service.SampleValidator;

@Configuration
public class TestApplication {
  @Autowired private RestTemplateBuilder restTemplateBuilder;

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  public ObjectMapper objectMapper() {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jackson2HalModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final MappingJackson2HttpMessageConverter halConverter =
        new TypeConstrainedMappingJackson2HttpMessageConverter(RepresentationModel.class);
    halConverter.setObjectMapper(mapper);
    halConverter.setSupportedMediaTypes(Collections.singletonList(MediaTypes.HAL_JSON));
    return mapper;
  }

  @Bean
  SampleValidator sampleValidator() {
    return null;
  }

  @Bean
  AapClientService aapClientService() {
    return null;
  }

  @Bean
  public ClientProperties bioSamplesProperties() {
    return new ClientProperties();
  }

  /* @Bean
  public OlsProcessor olsProcessor() {
    return new OlsProcessor(restTemplate(), bioSamplesProperties());
  }*/

  @Bean
  public CurationApplicationService curationApplicationService() {
    return new CurationApplicationService();
  }

  @Bean
  public BioSamplesClient bioSamplesClient() {
    return new MockBioSamplesClient(
        bioSamplesProperties().getBiosamplesClientUri(),
        bioSamplesProperties().getBiosamplesClientUriV2(),
        restTemplateBuilder,
        sampleValidator(),
        aapClientService(),
        bioSamplesProperties(),
        true);
  }
}
