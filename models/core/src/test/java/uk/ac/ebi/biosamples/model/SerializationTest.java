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
import com.google.common.collect.Sets;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.SortedSet;
import java.util.TreeSet;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@JsonTest
// @TestPropertySource(properties =
// {"spring.jackson.serialization.WRITE_DATES_AS_TIMESTAMPS=false","spring.jackson.serialization.WRITE_NULL_MAP_VALUES=false"})
@TestPropertySource(properties = {"spring.jackson.serialization.INDENT_OUTPUT=true"})
public class SerializationTest {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private JacksonTester<Sample> json;

  @Before
  public void setup() {
    final ObjectMapper objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
  }

  private Sample getSimpleSample() {
    final String name = "Test Sample";
    final String accession = "SAMEA1234";
    final String domain = "abcde12345";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");
    final Instant submitted = Instant.parse("2016-04-01T11:36:57.00Z");

    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    attributes.add(Attribute.build("age", "3", null, Lists.emptyList(), "year"));
    attributes.add(Attribute.build("organism part", "lung"));
    attributes.add(Attribute.build("organism part", "heart"));
    attributes.add(
        Attribute.build(
            "sex",
            "female",
            null,
            Sets.newHashSet(
                "http://purl.obolibrary.org/obo/PATO_0000383",
                "http://www.ebi.ac.uk/efo/EFO_0001265"),
            null));

    final SortedSet<Relationship> relationships = new TreeSet<>();
    relationships.add(Relationship.build("SAMEA1234", "derived from", "SAMD4321"));

    final SortedSet<ExternalReference> externalReferences = new TreeSet<>();
    externalReferences.add(ExternalReference.build("http://www.google.com"));

    final SortedSet<Organization> organizations = new TreeSet<>();
    //		organizations.add(Organization.build("Jo Bloggs Inc", "user", "help@jobloggs.com",
    // "http://www.jobloggs.com"));
    organizations.add(
        new Organization.Builder()
            .name("Jo Bloggs Inc")
            .role("user")
            .email("help@jobloggs.com")
            .url("http://www.jobloggs.com")
            .build());

    final SortedSet<Contact> contacts = new TreeSet<>();
    //		contacts.add(Contact.build("Joe Bloggs","Jo Bloggs Inc",
    // "http://www.jobloggs.com/joe"));
    contacts.add(
        new Contact.Builder()
            .firstName("Joe")
            .lastName("Bloggs")
            //                .affiliation("Jo Bloggs Inc")
            //				.url("http://www.jobloggs.com/joe")
            .name("Joe Bloggs")
            .role("Submitter")
            .email("jobloggs@joblogs.com")
            .build());

    final SortedSet<Publication> publications = new TreeSet<>();
    //		publications.add(Publication.build("10.1093/nar/gkt1081", "24265224"));
    publications.add(
        new Publication.Builder().doi("10.1093/nar/gkt1081").pubmed_id("24265224").build());

    //		return Sample.build(name, accession, domain, release, update, attributes, relationships,
    // externalReferences, organizations, contacts, publications);
    return new Sample.Builder(name, accession)
        .withDomain(domain)
        .withRelease(release)
        .withUpdate(update)
        .withSubmitted(submitted)
        .withAttributes(attributes)
        .withRelationships(relationships)
        .withExternalReferences(externalReferences)
        .withOrganizations(organizations)
        .withContacts(contacts)
        .withPublications(publications)
        .build();
  }

  @Test
  public void testSerialize() throws Exception {
    final Sample details = getSimpleSample();

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
    final Sample fileSample = json.readObject("/TEST1.json");
    final Sample simpleSample = getSimpleSample();
    log.info("fileSample = " + fileSample);
    log.info("simpleSample = " + simpleSample);
    // Use JSON path based assertions
    assertThat(fileSample.getName()).isEqualTo("Test Sample");
    assertThat(fileSample.getAccession()).isEqualTo("SAMEA1234");
    // Assert against a `.json` file
    assertThat(fileSample).isEqualTo(simpleSample);

    // check that a specific attribute exists
    assertThat(fileSample.getCharacteristics().contains(Attribute.build("organism part", "heart")));
  }

  @Test
  public void testRoundTrip() throws Exception {
    final Sample sample = getSimpleSample();
    log.info("roundTrip sample = " + sample);

    final String json = this.json.write(sample).getJson();
    log.info("roundTrip json = " + json);

    final InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    final Sample sampleRedux = this.json.readObject(inputStream);
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
