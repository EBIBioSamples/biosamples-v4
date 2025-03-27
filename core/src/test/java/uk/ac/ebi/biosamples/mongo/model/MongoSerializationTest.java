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
package uk.ac.ebi.biosamples.mongo.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataEntry;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;

@RunWith(SpringRunner.class)
@JsonTest
public class MongoSerializationTest {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private JacksonTester<MongoSample> json;
  private JacksonTester<MongoStructuredData> structuredDataJacksonTester;

  @Before
  public void setup() {
    final ObjectMapper objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
  }

  private static MongoSample getMongoSample() {
    final String name = "Test Sample";
    final String accession = "TEST1";
    final String sraAccession = "ERS01";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant create = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant submitted = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");
    final SubmittedViaType submittedVia = SubmittedViaType.JSON_API;

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

    final Set<AbstractData> structuredData = new HashSet<>();

    final SortedSet<MongoRelationship> relationships = new TreeSet<>();
    relationships.add(MongoRelationship.build("TEST1", "derived from", "TEST2"));

    final SortedSet<MongoExternalReference> externalReferences = new TreeSet<>();
    externalReferences.add(MongoExternalReference.build("http://www.google.com"));

    final SortedSet<Organization> organizations = new TreeSet<>();
    organizations.add(
        new Organization.Builder()
            .name("Jo Bloggs Inc")
            .role("user")
            .email("help@jobloggs.com")
            .url("http://www.jobloggs.com")
            .build());

    final SortedSet<Contact> contacts = new TreeSet<>();
    contacts.add(
        new Contact.Builder()
            .firstName("Joe")
            .lastName("Bloggs")
            .name("Joe Bloggs")
            .role("Submitter")
            .email("jobloggs@joblogs.com")
            .build());

    final SortedSet<Publication> publications = new TreeSet<>();
    //		publications.add(Publication.build("10.1093/nar/gkt1081", "24265224"));
    publications.add(
        new Publication.Builder().doi("10.1093/nar/gkt1081").pubmed_id("24265224").build());

    return MongoSample.build(
        name,
        accession,
        sraAccession,
        "foozit",
        "",
        Long.valueOf(9606),
        SampleStatus.PUBLIC,
        release,
        update,
        create,
        submitted,
        null,
        attributes,
        structuredData,
        relationships,
        externalReferences,
        organizations,
        contacts,
        publications,
        null,
        submittedVia);
  }

  private static MongoStructuredData getMongoStructuredData() {
    final String accession = "SAMEA000001";
    final String domain = "self.test";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant create = Instant.parse("2016-05-05T11:36:57.00Z");
    final Set<Map<String, StructuredDataEntry>> content = new HashSet<>();
    final StructuredDataTable table = StructuredDataTable.build(domain, null, "AMR", null, content);
    final Set<StructuredDataTable> data = new HashSet<>();
    data.add(table);

    final Map<String, StructuredDataEntry> row = new HashMap<>();
    row.put("antibioticName", StructuredDataEntry.build("ampicillin", "www.test.org"));
    row.put("resistancePhenotype", StructuredDataEntry.build("susceptible", null));
    row.put("vendor", StructuredDataEntry.build("in-house", null));
    row.put("laboratoryTypingMethod", StructuredDataEntry.build("MIC", null));
    row.put("astStandard", StructuredDataEntry.build("CLSI", null));
    row.put("measurementSign", StructuredDataEntry.build("==", null));
    row.put("measurement", StructuredDataEntry.build("2", null));
    row.put("measurementUnits", StructuredDataEntry.build("mg/L", null));
    content.add(row);

    return MongoStructuredData.build(accession, update, create, data);
  }

  @Test
  public void testSerialize() throws Exception {
    final MongoSample mongoSample = getMongoSample();

    // Use JSON path based assertions
    assertThat(json.write(mongoSample)).hasJsonPathStringValue("@.accession");
    assertThat(json.write(mongoSample))
        .extractingJsonPathStringValue("@.accession")
        .isEqualTo("TEST1");

    // Assert against a `.json` file in the same package as the test
    log.info("testSerialize() " + json.write(mongoSample).getJson());
    final MongoSample mongoSampleFromFile = json.readObject("/TEST1.json");
    log.info("testSerialize()-from file " + json.write(mongoSampleFromFile));
    assertThat(mongoSample.equals(mongoSampleFromFile));
  }

  @Test
  public void testDeserialize() throws Exception {
    // Use JSON path based assertions
    assertThat(json.readObject("/TEST1.json").getName()).isEqualTo("Test Sample");
    assertThat(json.readObject("/TEST1.json").getAccession()).isEqualTo("TEST1");
    // Assert against a `.json` file
    assertThat(json.readObject("/TEST1.json")).isEqualTo(getMongoSample());
  }

  @Test
  public void testSerialize_structured_data() throws Exception {
    final MongoStructuredData structuredData = getMongoStructuredData();

    assertThat(structuredDataJacksonTester.write(structuredData))
        .hasJsonPathStringValue("@.accession");

    assertThat(structuredDataJacksonTester.write(structuredData)).hasJsonPathArrayValue("@.data");
    assertThat(structuredDataJacksonTester.write(structuredData))
        .extractingJsonPathMapValue("@.data[0].content[0].antibioticName")
        .contains(new AbstractMap.SimpleEntry<>("value", "ampicillin"));

    assertThat(structuredDataJacksonTester.write(structuredData))
        .extractingJsonPathMapValue("@.data[0].content[0].antibioticName")
        .contains(new AbstractMap.SimpleEntry<>("iri", "www.test.org"));
  }

  @Configuration
  public static class TestConfig {}
}
