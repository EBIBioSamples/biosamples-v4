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

import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
public class XmlSearchIntegration extends AbstractIntegration {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final XmlSearchTester xmlSearchTester;

  public XmlSearchIntegration(final BioSamplesClient client) {
    super(client);

    xmlSearchTester = new XmlSearchTester(log, client);
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
  protected void phaseThree() {}

  @Override
  protected void phaseFour() {}

  @Override
  protected void phaseFive() {}

  @Override
  protected void phaseSix() {}

  private class XmlSearchTester {
    private final Logger log;
    private final BioSamplesClient client;

    XmlSearchTester(final Logger log, final BioSamplesClient client) {
      this.log = log;
      this.client = client;
    }

    void registerTestSamplesInBioSamples() {
      final List<Sample> baseSampleList =
          Arrays.asList(
              TestSampleGenerator.getRegularSample(),
              TestSampleGenerator.getPrivateSample(),
              TestSampleGenerator.getSampleGroup(),
              TestSampleGenerator.getSampleWithSpecificUpdateDate(),
              TestSampleGenerator.getSampleReleasedAtTheEndOfTheDay(),
              TestSampleGenerator.getSampleReleasedExaclyTheDayAfterSAMD0912312());

      for (Sample sample : baseSampleList) {
        /*throw new RuntimeException("Found existing " + sample.getAccession());*/

        final EntityModel<Sample> resource = client.persistSampleResource(sample);
        final Sample sampleContent = resource.getContent();
        final Attribute sraAccessionAttribute =
            sampleContent.getAttributes().stream()
                .filter(attribute -> attribute.getType().equals("SRA accession"))
                .findFirst()
                .get();

        sample.getAttributes().add(sraAccessionAttribute);

        sample =
            Sample.Builder.fromSample(sample)
                .withStatus(sampleContent.getStatus())
                .withAccession(Objects.requireNonNull(resource.getContent()).getAccession())
                .withSraAccession(Objects.requireNonNull(resource.getContent()).getSraAccession())
                .build();

        if (!sampleContent.equals(sample)) {
          throw new IntegrationTestFailException(
              "Expected response (" + sampleContent + ") to equal submission (" + sample + ")",
              Phase.ONE);
        }
      }

      final Sample sampleWithinGroup = TestSampleGenerator.getSampleWithinGroup();

      Optional<EntityModel<Sample>> optional =
          client.fetchSampleResource(sampleWithinGroup.getAccession());

      if (optional.isPresent()) {
        throw new RuntimeException("Found existing " + sampleWithinGroup.getAccession());
      }

      final EntityModel<Sample> sampleWithinGroupResource =
          client.persistSampleResource(sampleWithinGroup);
      // The result and the submitted will not be equal because of the new inverse relation
      // created
      // automatically
      if (!sampleWithinGroup
          .getAccession()
          .equals(Objects.requireNonNull(sampleWithinGroupResource.getContent()).getAccession())) {
        throw new RuntimeException("Expected response to equal submission - accession mismatch");
      }

      final Sample sampleWithContactInformations =
          TestSampleGenerator.getSampleWithContactInformations();

      log.info(String.format("Persisting %s", sampleWithContactInformations.getAccession()));

      optional = client.fetchSampleResource(sampleWithContactInformations.getAccession());

      if (optional.isPresent()) {
        throw new RuntimeException(
            "Found existing " + sampleWithContactInformations.getAccession());
      }

      final EntityModel<Sample> sampleWithContactResource =
          client.persistSampleResource(sampleWithContactInformations);
      // The result and the submitted will not be equal because of the new inverse relation
      // created
      // automatically
      if (!sampleWithContactInformations
          .getAccession()
          .equals(Objects.requireNonNull(sampleWithContactResource.getContent()).getAccession())) {
        throw new RuntimeException("Expected response to equal submission");
      }

      log.info(
          String.format("Successfully persisted %s", sampleWithContactInformations.getAccession()));

      final Sample groupWithMsiData = TestSampleGenerator.getGroupWithFullMsiDetails();

      log.info(String.format("Persisting %s", groupWithMsiData.getAccession()));

      optional = client.fetchSampleResource(groupWithMsiData.getAccession());

      if (optional.isPresent()) {
        throw new RuntimeException("Found existing " + groupWithMsiData.getAccession());
      }

      final EntityModel<Sample> groupWithMsiDetailsResource =
          client.persistSampleResource(groupWithMsiData);
      // The result and the submitted will not be equal because of the new inverse relation
      // created
      // automatically
      if (!groupWithMsiData
          .getAccession()
          .equals(
              Objects.requireNonNull(groupWithMsiDetailsResource.getContent()).getAccession())) {
        throw new RuntimeException("Expected response to equal submission");
      }

      log.info(String.format("Successfully persisted %s", groupWithMsiData.getAccession()));
    }

    void triesToFindSampleUsingClient() {
      final Sample test1 = TestSampleGenerator.getRegularSample();

      log.info("Check existence of sample " + test1.getAccession());

      final Optional<EntityModel<Sample>> optional =
          client.fetchSampleResource(test1.getAccession());

      if (!optional.isPresent()) {
        throw new RuntimeException("Expected sample not found " + test1.getAccession());
      }

      log.info("Sample " + test1.getAccession() + " found correctly");
    }
  }

  private static class TestSampleGenerator {
    static Sample getRegularSample() {
      final String name = "Test XML Sample";
      final String accession = "SAMEA999999";
      final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
      final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");
      final SortedSet<Attribute> attributes = new TreeSet<>();

      attributes.add(
          Attribute.build(
              "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
      //            return Sample.build(name, accession, "self.BiosampleIntegrationTest",
      // release,
      // update, attributes, new TreeSet<>(), new TreeSet<>(), null, null, null);
      return new Sample.Builder(name, accession)
          .withTaxId(9606L)
          .withWebinSubmissionAccountId(defaultWebinIdForIntegrationTests)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(attributes)
          .build();
    }

    static Sample getPrivateSample() {
      final String name = "Private XML sample";
      final String accession = "SAMEA888888";
      final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
      final Instant release = Instant.parse("2116-04-01T11:36:57.00Z");
      final SortedSet<Attribute> attributes = new TreeSet<>();

      attributes.add(
          Attribute.build(
              "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

      //            return Sample.build(name, accession, "self.BiosampleIntegrationTest",
      // release,
      // update, attributes, new TreeSet<>(), new TreeSet<>(), null, null, null);
      return new Sample.Builder(name, accession)
          .withTaxId(9606L)
          .withWebinSubmissionAccountId(defaultWebinIdForIntegrationTests)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(attributes)
          .build();
    }

    static Sample getSampleWithinGroup() {
      final String name = "Sample part of group";
      final String accession = "SAMEA777777";
      final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
      final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

      //            SortedSet<Attribute> attributes = new TreeSet<>();

      //            return Sample.build(name, accession, "self.BiosampleIntegrationTest",
      // release,
      // update, attributes, new TreeSet<>(), new TreeSet<>(), null, null, null);
      return new Sample.Builder(name, accession)
          .withWebinSubmissionAccountId(defaultWebinIdForIntegrationTests)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(Collections.singleton(Attribute.build("Organism", "Human")))
          .build();
    }

    static Sample getSampleGroup() {
      final String name = "Test XML sample group";
      final String accession = "SAMEG001122";
      final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
      final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");
      final SortedSet<Relationship> relationships = new TreeSet<>();

      relationships.add(
          Relationship.build(accession, "has member", getSampleWithinGroup().getAccession()));

      //            return Sample.build(name, accession, "self.BiosampleIntegrationTest",
      // release,
      // update, new TreeSet<>(), relationships, new TreeSet<>(), null, null, null);
      return new Sample.Builder(name, accession)
          .withWebinSubmissionAccountId(defaultWebinIdForIntegrationTests)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(Collections.singleton(Attribute.build("Organism", "Human")))
          .withRelationships(relationships)
          .build();
    }

    static Sample getSampleWithSpecificUpdateDate() {
      final String name = "Test XML sample for update date";
      final String accession = "SAME101010";
      final Instant update = Instant.now();
      final Instant release = Instant.parse("1980-08-02T00:30:00Z");
      //            return Sample.build(name, accession, submissionDomain, release, update,
      // null,
      //                    null, null, null, null,
      //                    null);
      return new Sample.Builder(name, accession)
          .withWebinSubmissionAccountId(defaultWebinIdForIntegrationTests)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(Collections.singleton(Attribute.build("Organism", "Human")))
          .build();
    }

    static Sample getSampleWithContactInformations() {
      final String name = "Test XML sample with contact information";
      final String accession = "SAME114477";
      final Instant update = Instant.now();
      final Instant release = Instant.parse("1980-08-02T00:30:00Z");
      final SortedSet<Contact> contacts = new TreeSet<>();

      contacts.add(new Contact.Builder().firstName("Loca").lastName("Lol").build());

      //            return Sample.build(name, accession, submissionDomain, release, update,
      //                    null, null, null,
      //                    null, contacts, null);
      return new Sample.Builder(name, accession)
          .withWebinSubmissionAccountId(defaultWebinIdForIntegrationTests)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(Collections.singleton(Attribute.build("Organism", "Human")))
          .withContacts(contacts)
          .build();
    }

    static Sample getGroupWithFullMsiDetails() {
      final String name = "Test XML group with contact information and other";
      final String accession = "SAMEG114477";
      final Instant update = Instant.now();
      final Instant release = Instant.parse("1980-08-02T00:30:00Z");
      final SortedSet<Contact> contacts = new TreeSet<>();

      contacts.add(new Contact.Builder().firstName("Loca").lastName("Lol").build());

      final SortedSet<Organization> organizations = new TreeSet<>();

      organizations.add(
          new Organization.Builder()
              .name("testOrg")
              .role("submitter")
              .email("test@org.com")
              .address("rue de german")
              .url("www.google.com")
              .build());

      final SortedSet<Publication> publications = new TreeSet<>();

      publications.add(new Publication.Builder().doi("123123").pubmed_id("someID").build());

      //            return Sample.build(name, accession, submissionDomain, release, update,
      //                    null, null, null,
      //                    organizations, contacts, publications);
      return new Sample.Builder(name, accession)
          .withWebinSubmissionAccountId(defaultWebinIdForIntegrationTests)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(Collections.singleton(Attribute.build("Organism", "Human")))
          .withOrganizations(organizations)
          .withContacts(contacts)
          .withPublications(publications)
          .build();
    }

    static Sample getSampleReleasedAtTheEndOfTheDay() {
      final String name = "Test XML Sample with release date almost at the end of the day";
      final String accession = "SAMD0912312";
      final Instant update = Instant.now();
      final Instant release = Instant.parse("2016-08-02T23:59:59Z");

      //            return Sample.build(name, accession, submissionDomain, release, update,
      //                    null, null, null,
      //                    null, null, null);
      return new Sample.Builder(name, accession)
          .withWebinSubmissionAccountId(defaultWebinIdForIntegrationTests)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(Collections.singleton(Attribute.build("Organism", "Human")))
          .build();
    }

    static Sample getSampleReleasedExaclyTheDayAfterSAMD0912312() {
      final String name = "Test XML Sample SAMD0912313";
      final String accession = "SAMD0912313";
      final Instant update = Instant.now();
      final Instant release = Instant.parse("2016-08-03T00:00:00Z");
      final SortedSet<Attribute> attributes = new TreeSet<>();

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
          .withWebinSubmissionAccountId(defaultWebinIdForIntegrationTests)
          .withRelease(release)
          .withUpdate(update)
          .withAttributes(attributes)
          .build();
    }
  }
}
