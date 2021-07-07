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
package uk.ac.ebi.biosamples;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

import java.io.StringReader;
import java.time.Instant;
import java.util.*;

@Component
public class XmlSearchIntegration extends AbstractIntegration {

  private final XmlSearchTester xmlSearchTester;

  Logger log = LoggerFactory.getLogger(getClass());

  public XmlSearchIntegration(
      BioSamplesClient client,
      RestTemplateBuilder restTemplateBuilder,
      IntegrationProperties integrationProperties) {
    super(client);
    RestTemplate restTemplate = restTemplateBuilder.build();
    this.xmlSearchTester = new XmlSearchTester(log, client, restTemplate, integrationProperties);
  }

  @Override
  protected void phaseOne() {

    xmlSearchTester.registerTestSamplesInBioSamples();
  }

  @Override
  protected void phaseTwo() {

    xmlSearchTester.triesToFindSampleUsingClient();
  }

  @Override
  protected void phaseThree() {
  }

  @Override
  protected void phaseFour() {
  }

  @Override
  protected void phaseFive() {
  }

  private static class XmlSearchTester {

    private final IntegrationProperties integrationProperties;
    private final Logger log;
    private final BioSamplesClient client;
    private final RestTemplate restTemplate;

    public XmlSearchTester(
        Logger log,
        BioSamplesClient client,
        RestTemplate restTemplate,
        IntegrationProperties integrationProperties) {
      this.log = log;
      this.client = client;
      this.restTemplate = restTemplate;
      this.integrationProperties = integrationProperties;
    }

    public void registerTestSamplesInBioSamples() {

      List<Sample> baseSampleList =
          Arrays.asList(
              TestSampleGenerator.getRegularSample(),
              TestSampleGenerator.getPrivateSample(),
              TestSampleGenerator.getSampleGroup(),
              TestSampleGenerator.getSampleWithSpecificUpdateDate(),
              TestSampleGenerator.getSampleReleasedAtTheEndOfTheDay(),
              TestSampleGenerator.getSampleReleasedExaclyTheDayAfterSAMD0912312());

      for (Sample sample : baseSampleList) {
        Optional<Resource<Sample>> optional = client.fetchSampleResource(sample.getAccession());
        if (optional.isPresent()) {
          throw new RuntimeException("Found existing " + sample.getAccession());
        }

        Resource<Sample> resource = client.persistSampleResource(sample);
        if (!sample.equals(resource.getContent())) {
          throw new RuntimeException("Expected response to equal submission");
        }
      }

      Sample sampleWithinGroup = TestSampleGenerator.getSampleWithinGroup();
      Optional<Resource<Sample>> optional =
          client.fetchSampleResource(sampleWithinGroup.getAccession());
      if (optional.isPresent()) {
        throw new RuntimeException("Found existing " + sampleWithinGroup.getAccession());
      }

      Resource<Sample> sampleWithinGroupResource = client.persistSampleResource(sampleWithinGroup);
      // The result and the submitted will not be equal because of the new inverse relation
      // created
      // automatically
      if (!sampleWithinGroup
          .getAccession()
          .equals(sampleWithinGroupResource.getContent().getAccession())) {
        throw new RuntimeException("Expected response to equal submission");
      }

      Sample sampleWithContactInformations = TestSampleGenerator.getSampleWithContactInformations();
      log.info(String.format("Persisting %s", sampleWithContactInformations.getAccession()));
      optional = client.fetchSampleResource(sampleWithContactInformations.getAccession());
      if (optional.isPresent()) {
        throw new RuntimeException(
            "Found existing " + sampleWithContactInformations.getAccession());
      }

      Resource<Sample> sampleWithContactResource =
          client.persistSampleResource(sampleWithContactInformations, false, true);
      // The result and the submitted will not be equal because of the new inverse relation
      // created
      // automatically
      if (!sampleWithContactInformations
          .getAccession()
          .equals(sampleWithContactResource.getContent().getAccession())) {
        throw new RuntimeException("Expected response to equal submission");
      }
      log.info(
          String.format("Successfully persisted %s", sampleWithContactInformations.getAccession()));

      Sample groupWithMsiData = TestSampleGenerator.getGroupWithFullMsiDetails();
      log.info(String.format("Persisting %s", groupWithMsiData.getAccession()));
      optional = client.fetchSampleResource(groupWithMsiData.getAccession());
      if (optional.isPresent()) {
        throw new RuntimeException("Found existing " + groupWithMsiData.getAccession());
      }

      Resource<Sample> groupWithMsiDetailsResource =
          client.persistSampleResource(groupWithMsiData, false, true);
      // The result and the submitted will not be equal because of the new inverse relation
      // created
      // automatically
      if (!groupWithMsiData
          .getAccession()
          .equals(groupWithMsiDetailsResource.getContent().getAccession())) {
        throw new RuntimeException("Expected response to equal submission");
      }
      log.info(String.format("Successfully persisted %s", groupWithMsiData.getAccession()));
    }

    public void triesToFindSampleUsingClient() {
      Sample test1 = TestSampleGenerator.getRegularSample();

      log.info("Check existence of sample " + test1.getAccession());

      Optional<Resource<Sample>> optional = client.fetchSampleResource(test1.getAccession());
      if (!optional.isPresent()) {
        throw new RuntimeException("Expected sample not found " + test1.getAccession());
      }

      log.info("Sample " + test1.getAccession() + " found correctly");
    }
  }

  private static class TestSampleGenerator {

    public static Sample getRegularSample() {
      String name = "Test XML Sample";
      String accession = "SAMEA999999";
      String domain = "self.BiosampleIntegrationTest";
      Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
      Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

      SortedSet<Attribute> attributes = new TreeSet<>();
      attributes.add(
          Attribute.build(
              "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
      //            return Sample.build(name, accession, "self.BiosampleIntegrationTest",
      // release,
      // update, attributes, new TreeSet<>(), new TreeSet<>(), null, null, null);
      return new Sample.Builder(name, accession)
          .withDomain(domain)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(attributes)
          .build();
    }

    public static Sample getPrivateSample() {
      String name = "Private XML sample";
      String accession = "SAMEA888888";
      String domain = "self.BiosampleIntegrationTest";
      Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
      Instant release = Instant.parse("2116-04-01T11:36:57.00Z");

      SortedSet<Attribute> attributes = new TreeSet<>();
      attributes.add(
          Attribute.build(
              "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

      //            return Sample.build(name, accession, "self.BiosampleIntegrationTest",
      // release,
      // update, attributes, new TreeSet<>(), new TreeSet<>(), null, null, null);
      return new Sample.Builder(name, accession)
          .withDomain(domain)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(attributes)
          .build();
    }

    public static Sample getSampleWithinGroup() {
      String name = "Sample part of group";
      String accession = "SAMEA777777";
      String domain = "self.BiosampleIntegrationTest";
      Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
      Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

      //            SortedSet<Attribute> attributes = new TreeSet<>();

      //            return Sample.build(name, accession, "self.BiosampleIntegrationTest",
      // release,
      // update, attributes, new TreeSet<>(), new TreeSet<>(), null, null, null);

      return new Sample.Builder(name, accession)
          .withDomain(domain)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(Collections.singleton(Attribute.build("Organism", "Human")))
          .build();
    }

    public static Sample getSampleGroup() {
      String name = "Test XML sample group";
      String accession = "SAMEG001122";
      String domain = "self.BiosampleIntegrationTest";
      Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
      Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

      SortedSet<Relationship> relationships = new TreeSet<>();
      relationships.add(
          Relationship.build(accession, "has member", getSampleWithinGroup().getAccession()));

      //            return Sample.build(name, accession, "self.BiosampleIntegrationTest",
      // release,
      // update, new TreeSet<>(), relationships, new TreeSet<>(), null, null, null);
      return new Sample.Builder(name, accession)
          .withDomain(domain)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(Collections.singleton(Attribute.build("Organism", "Human")))
          .withRelationships(relationships)
          .build();
    }

    public static Sample getSampleWithSpecificUpdateDate() {
      String name = "Test XML sample for update date";
      String accession = "SAME101010";
      String domain = "self.BiosampleIntegrationTest";
      Instant update = Instant.now();
      Instant release = Instant.parse("1980-08-02T00:30:00Z");

      //            return Sample.build(name, accession, submissionDomain, release, update,
      // null,
      //                    null, null, null, null,
      //                    null);
      return new Sample.Builder(name, accession)
          .withDomain(domain)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(Collections.singleton(Attribute.build("Organism", "Human")))
          .build();
    }

    public static Sample getSampleWithContactInformations() {
      String name = "Test XML sample with contact information";
      String accession = "SAME114477";
      String domain = "self.BiosampleIntegrationTest";
      Instant update = Instant.now();
      Instant release = Instant.parse("1980-08-02T00:30:00Z");

      SortedSet<Contact> contacts = new TreeSet<>();
      contacts.add(new Contact.Builder().firstName("Loca").lastName("Lol").build());

      //            return Sample.build(name, accession, submissionDomain, release, update,
      //                    null, null, null,
      //                    null, contacts, null);
      return new Sample.Builder(name, accession)
          .withDomain(domain)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(Collections.singleton(Attribute.build("Organism", "Human")))
          .withContacts(contacts)
          .build();
    }

    public static Sample getGroupWithFullMsiDetails() {
      String name = "Test XML group with contact information and other";
      String accession = "SAMEG114477";
      String domain = "self.BiosampleIntegrationTest";
      Instant update = Instant.now();
      Instant release = Instant.parse("1980-08-02T00:30:00Z");

      SortedSet<Contact> contacts = new TreeSet<>();
      contacts.add(new Contact.Builder().firstName("Loca").lastName("Lol").build());

      SortedSet<Organization> organizations = new TreeSet<>();
      organizations.add(
          new Organization.Builder()
              .name("testOrg")
              .role("submitter")
              .email("test@org.com")
              .address("rue de german")
              .url("www.google.com")
              .build());

      SortedSet<Publication> publications = new TreeSet<>();
      publications.add(new Publication.Builder().doi("123123").pubmed_id("someID").build());

      //            return Sample.build(name, accession, submissionDomain, release, update,
      //                    null, null, null,
      //                    organizations, contacts, publications);
      return new Sample.Builder(name, accession)
          .withDomain(domain)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(Collections.singleton(Attribute.build("Organism", "Human")))
          .withOrganizations(organizations)
          .withContacts(contacts)
          .withPublications(publications)
          .build();
    }

    public static Sample getSampleReleasedAtTheEndOfTheDay() {
      String name = "Test XML Sample with release date almost at the end of the day";
      String accession = "SAMD0912312";
      String domain = "self.BiosampleIntegrationTest";
      Instant update = Instant.now();
      Instant release = Instant.parse("2016-08-02T23:59:59Z");

      //            return Sample.build(name, accession, submissionDomain, release, update,
      //                    null, null, null,
      //                    null, null, null);
      return new Sample.Builder(name, accession)
          .withDomain(domain)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(Collections.singleton(Attribute.build("Organism", "Human")))
          .build();
    }

    public static Sample getSampleReleasedExaclyTheDayAfterSAMD0912312() {
      String name = "Test XML Sample SAMD0912313";
      String accession = "SAMD0912313";
      String domain = "self.BiosampleIntegrationTest";
      Instant update = Instant.now();
      Instant release = Instant.parse("2016-08-03T00:00:00Z");

      SortedSet<Attribute> attributes = new TreeSet<>();
      attributes.add(
          new Attribute.Builder(
                  "description",
                  "Sample released exactly at midnight of the day after another sample was released")
              .build());
      attributes.add(Attribute.build("Organism", "Human"));

      //            return Sample.build(name, accession, submissionDomain, release, update,
      //                    attributes, null, null,
      //                    null, null, null);
      return new Sample.Builder(name, accession)
          .withDomain(domain)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(attributes)
          .build();
    }
  }

  private static class XmlMatcher {

    public static boolean matchesSampleInGroup(String serializedXml, Sample sampleInGroup) {
      SAXReader reader = new SAXReader();

      Document xml;
      try {
        xml = reader.read(new StringReader(serializedXml));
      } catch (DocumentException e) {
        return false;
      }

      Element root = xml.getRootElement();
      if (!root.getName().equals("ResultQuery")) {
        return false;
      }

      String sampleWithinGroupAccession = XmlPathBuilder.of(root).path("BioSample").attribute("id");

      return sampleWithinGroupAccession.equals(sampleInGroup.getAccession());
    }

    public static boolean matchesSample(String serializedXml, Sample referenceSample) {

      SAXReader reader = new SAXReader();

      Document xml;
      try {
        xml = reader.read(new StringReader(serializedXml));
      } catch (DocumentException e) {
        return false;
      }

      Element root = xml.getRootElement();
      if (!XmlPathBuilder.of(root).element().getName().equals("BioSample")
          && !XmlPathBuilder.of(root).element().getName().equals("BioSampleGroup")) {
        return false;
      }

      String sampleAccession = XmlPathBuilder.of(root).attribute("id");
      Element sampleNameElement = XmlPathBuilder.of(root).path("Property").element();
      String sampleName =
          XmlPathBuilder.of(sampleNameElement).path("QualifiedValue", "Value").text();

      return sampleAccession.equals(referenceSample.getAccession())
          && sampleName.equals(referenceSample.getName());
    }
  }
}
