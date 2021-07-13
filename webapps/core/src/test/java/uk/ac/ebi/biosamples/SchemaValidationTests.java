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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.Charset;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SchemaValidationTests {

  @Autowired private MockMvc mockMvc;

  @Test
  public void get_validation_endpoint_return_not_allowed_response() throws Exception {
    mockMvc
        .perform(get("/validate").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isMethodNotAllowed());
  }

  @Test
  public void expect_schemas_to_exist_and_return_json_file_with_title_amr() throws Exception {
    mockMvc
        .perform(get("/schemas/amr.json"))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
        .andExpect(jsonPath("$.title").value("amr"));
  }

  @Test
  @Ignore // Impossible to test as a unit test (unless by mocking the validation service), need to
  // be tested by an integration test
  public void validates_provided_json_against_test_schema_correctly() throws Exception {

    String jsonContent =
        StreamUtils.copyToString(
            new ClassPathResource("json_schema_valid_object.json").getInputStream(),
            Charset.defaultCharset());

    mockMvc
        .perform(
            post("/validation").contentType(MediaType.APPLICATION_JSON_VALUE).content(jsonContent))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().json("[]"));
  }

  @Test
  @Ignore // Impossible to test as a unit test, need to be tested by an integration test
  public void reject_not_valid_json() throws Exception {

    String invalidJsonContent =
        StreamUtils.copyToString(
            new ClassPathResource("json_schema_not_valid_object.json").getInputStream(),
            Charset.defaultCharset());

    mockMvc
        .perform(
            post("/validation")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(invalidJsonContent))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").value(hasSize(4)))
        .andExpect(
            content()
                .json(
                    "[\n"
                        + "    {\n"
                        + "        \"dataPath\": \"[0].antibiotic\",\n"
                        + "        \"errors\": [\n"
                        + "            \"should have required property 'antibiotic'\"\n"
                        + "        ]\n"
                        + "    },\n"
                        + "    {\n"
                        + "        \"dataPath\": \"[0].measurement_sign\",\n"
                        + "        \"errors\": [\n"
                        + "            \"should have required property 'measurement_sign'\"\n"
                        + "        ]\n"
                        + "    },\n"
                        + "    {\n"
                        + "        \"dataPath\": \"[0].measurement\",\n"
                        + "        \"errors\": [\n"
                        + "            \"should be string\"\n"
                        + "        ]\n"
                        + "    },\n"
                        + "    {\n"
                        + "        \"dataPath\": \"[0].measurement_units\",\n"
                        + "        \"errors\": [\n"
                        + "            \"should have required property 'measurement_units'\"\n"
                        + "        ]\n"
                        + "    }\n"
                        + "]"));
  }
}
