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
package uk.ac.ebi.biosamples.solr;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.Test;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

public class MessageHandlerSolrTest {

  @Test
  public void should_index_public_sample() throws Exception {
    Attribute attribute = Attribute.build("INSDC status", "public");
    assertTrue(
        MessageHandlerSolr.isIndexingCandidate(
            generateTestSample("public-example", Collections.singletonList(attribute))));
  }

  @Test
  public void should_index_live_sample() throws Exception {
    Attribute attribute = Attribute.build("INSDC status", "live");
    assertTrue(
        MessageHandlerSolr.isIndexingCandidate(
            (generateTestSample("live-example", Collections.singletonList(attribute)))));
  }

  @Test
  public void should_index_sample_with_no_INSDC_status() throws Exception {
    assertTrue(
        MessageHandlerSolr.isIndexingCandidate(
            (generateTestSample("no-example", Collections.EMPTY_LIST))));
  }

  @Test
  public void should_index_SAMEA5397449_sample_with_no_INSDC_status() throws Exception {
    String filePath = "/examples/samples/SAMEA5397449.json";
    ObjectMapper objectMapper = getObjectMapper();
    Sample sample =
        objectMapper.readValue(
            MessageHandlerSolrTest.class.getResourceAsStream(filePath), Sample.class);
    assertTrue(MessageHandlerSolr.isIndexingCandidate(sample));
  }

  public ObjectMapper getObjectMapper() {
    RestTemplate restTemplate = new RestTemplate();
    List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jackson2HalModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    MappingJackson2HttpMessageConverter halConverter =
        new TypeConstrainedMappingJackson2HttpMessageConverter(ResourceSupport.class);
    halConverter.setObjectMapper(mapper);
    halConverter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));
    // make sure this is inserted first
    converters.add(0, halConverter);
    restTemplate.setMessageConverters(converters);
    return mapper;
  }

  @Test
  public void should_not_index_suppressed_sample() {
    Attribute attribute = Attribute.build("INSDC status", "suppressed");
    assertTrue(
        MessageHandlerSolr.isIndexingCandidate(
            (generateTestSample("suppressed-example", Collections.singletonList(attribute)))));
  }

  @Test
  public void should_not_index_sample_with_unexpected_INSDC_status() {
    Attribute attribute = Attribute.build("INSDC status", "gertgerge");
    assertFalse(
        MessageHandlerSolr.isIndexingCandidate(
            (generateTestSample("unexpected-example", Collections.singletonList(attribute)))));
  }

  private Sample generateTestSample(String accession, List<Attribute> attributes) {
    Set<Attribute> attributeSet = new HashSet<>();
    for (Attribute attribute : attributes) {
      attributeSet.add(attribute);
    }
    return Sample.build(
        "",
        accession,
        "",
        "",
        Long.valueOf(9606),
        Instant.now(),
        Instant.now(),
        Instant.now(),
        Instant.now(),
        Instant.now(),
        attributeSet,
        Collections.emptySet(),
        Collections.emptySet());
  }
}
