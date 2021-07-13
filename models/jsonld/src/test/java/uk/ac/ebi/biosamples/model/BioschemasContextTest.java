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
package uk.ac.ebi.biosamples.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@JsonTest
// @TestPropertySource(properties =
// {"spring.jackson.serialization.WRITE_DATES_AS_TIMESTAMPS=false","spring.jackson.serialization.WRITE_NULL_MAP_VALUES=false"})
@TestPropertySource(properties = {"spring.jackson.serialization.INDENT_OUTPUT=true"})
public class BioschemasContextTest {

  private Logger log = LoggerFactory.getLogger(this.getClass());

  private JacksonTester<BioSchemasContext> json;

  @Test
  public void testDeserialize() throws Exception {
    String contextJson =
        "["
            + "\"http://schema.org\","
            + "{\"OBI\":\"http://purl.obolibrary.org/obo/OBI_\",\"biosample\":\"http://identifiers.org/biosample\"}"
            + "]";

    BioSchemasContext context =
        new ObjectMapper().readerFor(BioSchemasContext.class).readValue(contextJson);

    // Use JSON path based assertions
    assertThat(context.getSchemaOrgContext()).isEqualTo(URI.create("http://schema.org"));
  }

  @Test
  public void testSerialize() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    BioSchemasContext context = new BioSchemasContext();
    context.addOtherContexts("OBI", URI.create("http://purl.obolibrary.org/obo/OBI_"));
    context.addOtherContexts("biosample", URI.create("http://identifiers.org/biosample"));
    String actualJson = mapper.writeValueAsString(context);

    String expectedJson =
        "["
            + "\"http://schema.org\","
            + "{\"OBI\":\"http://purl.obolibrary.org/obo/OBI_\",\"biosample\":\"http://identifiers.org/biosample\"}"
            + "]";

    JSONAssert.assertEquals(
        expectedJson,
        actualJson,
        new CustomComparator(
            JSONCompareMode.LENIENT, new Customization("metaData.created", (o1, o2) -> true)));
  }

  @SpringBootConfiguration
  public static class TestConfig {}
}
