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
package uk.ac.ebi.biosamples.ena;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.service.AapClientService;
import uk.ac.ebi.biosamples.client.service.WebinAuthClientService;
import uk.ac.ebi.biosamples.client.utils.ClientProperties;
import uk.ac.ebi.biosamples.service.CurationApplicationService;
import uk.ac.ebi.biosamples.service.SampleValidator;

@Configuration
public class TestApplication {
  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  public RestTemplateBuilder restTemplateBuilder() {
    return new RestTemplateBuilder();
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
  WebinAuthClientService webinAuthClientService() {
    return null;
  }

  @Bean
  public ClientProperties clientProperties() {
    return new ClientProperties();
  }

  @Bean
  public CurationApplicationService curationApplicationService() {
    return new CurationApplicationService();
  }

  @Bean("MOCKCLIENT")
  public BioSamplesClient bioSamplesClient() {
    return new MockBioSamplesClient(
        clientProperties().getBiosamplesClientUri(),
        clientProperties().getBiosamplesClientUriV2(),
        restTemplateBuilder(),
        sampleValidator(),
        webinAuthClientService(),
        clientProperties(),
        objectMapper());
  }

  @Bean
  public DataSource getDataSource() {
    final DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.driverClassName("oracle.jdbc.OracleDriver");
    dataSourceBuilder.url("jdbc:oracle:thin:@//ora-era-pro-hl.ebi.ac.uk:1531/ERAPRO");
    dataSourceBuilder.username("era_reader");
    dataSourceBuilder.password("reader");

    return dataSourceBuilder.build();
  }

  @Bean("eraJdbcTemplate")
  public JdbcTemplate jdbcTemplate() {
    return new JdbcTemplate(getDataSource());
  }
}
