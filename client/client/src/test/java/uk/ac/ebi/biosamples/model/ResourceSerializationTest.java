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
import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.EntityModel;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.core.model.*;

@RunWith(SpringRunner.class)
@JsonTest
@TestPropertySource(properties = {"spring.jackson.serialization.INDENT_OUTPUT=true"})
public class ResourceSerializationTest {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private JacksonTester<EntityModel<Sample>> json;

  public ResourceSerializationTest() {}

  @Before
  public void setup() {
    final ObjectMapper objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
  }

  private Sample getSimpleSample() {
    final String name = "Test Sample";
    final String accession = "SAMEA1234";
    final String webinId = "Webin-12345";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism",
            "Homo sapiens",
            null,
            Lists.newArrayList("http://purl.obolibrary.org/obo/NCBITaxon_9606"),
            null));
    attributes.add(Attribute.build("age", "3", null, Collections.emptyList(), "year"));
    attributes.add(Attribute.build("organism part", "lung"));
    attributes.add(Attribute.build("organism part", "heart"));

    final SortedSet<Relationship> relationships = new TreeSet<>();
    relationships.add(Relationship.build("SAMEA1234", "derived from", "SAMD4321"));

    final SortedSet<ExternalReference> externalReferences = new TreeSet<>();
    externalReferences.add(ExternalReference.build("http://www.google.com"));

    //		return Sample.build(name, accession, domain, release, update, attributes, relationships,
    // externalReferences, null, null, null);
    return new Sample.Builder(name, accession)
        .withWebinSubmissionAccountId(webinId)
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(attributes)
        .withRelationships(relationships)
        .withExternalReferences(externalReferences)
        .build();
  }

  @Test
  public void testSerialize() throws Exception {
    final EntityModel<Sample> details = EntityModel.of(getSimpleSample());

    log.info(json.write(details).getJson());

    // Use JSON path based assertions
    assertThat(json.write(details)).hasJsonPathStringValue("@.accession");
    assertThat(json.write(details))
        .extractingJsonPathStringValue("@.accession")
        .isEqualTo("SAMEA1234");

    // Assert against a `.json` file in the same package as the test
    assertThat(json.write(details)).isEqualToJson("/TEST1.json");
  }

  @Test
  public void testDeserialize() throws Exception {
    final EntityModel<Sample> fileSample = json.readObject("/TEST1.json");
    final EntityModel<Sample> simpleSample = EntityModel.of(getSimpleSample());

    log.info("fileSample = " + fileSample);
    log.info("simpleSample = " + simpleSample);
    // Use JSON path based assertions

    assertThat(Objects.requireNonNull(fileSample.getContent()).getName()).isEqualTo("Test Sample");
    assertThat(fileSample.getContent().getAccession()).isEqualTo("SAMEA1234");
    // Assert against a `.json` file
    assertThat(fileSample).isEqualTo(simpleSample);

    // check that a specific attribute exists
    assertThat(
        fileSample
            .getContent()
            .getCharacteristics()
            .contains(Attribute.build("organism part", "heart")));
  }

  @Test
  public void testRoundTrip() throws Exception {
    final EntityModel<Sample> sample = EntityModel.of(getSimpleSample());
    log.info("roundTrip sample = " + sample);

    final String json = this.json.write(sample).getJson();
    log.info("roundTrip json = " + json);

    final InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    final EntityModel<Sample> sampleRedux = this.json.readObject(inputStream);
    log.info("roundTrip sampleRedux = " + sampleRedux);

    final String jsonRedux = this.json.write(sampleRedux).getJson();
    log.info("roundTrip jsonRedux = " + jsonRedux);

    final BufferedReader br =
        new BufferedReader(
            new InputStreamReader(new ClassPathResource("/TEST1.json").getInputStream()), 1024);
    final StringBuilder stringBuilder = new StringBuilder();

    String line;

    while ((line = br.readLine()) != null) {
      stringBuilder.append(line).append('\n');
    }

    br.close();
    final String jsonFile = stringBuilder.toString();

    assertThat(sample.equals(sampleRedux));
    assertThat(sample.equals(jsonFile));
    assertThat(json.equals(jsonRedux));
    assertThat(json.equals(jsonFile));
  }

  @SpringBootConfiguration
  public static class TestConfig {}
}
