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

import com.google.common.collect.Sets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
@Order(2)
public class RestIntegrationSRAV2 extends AbstractIntegration {
  public static final String SRA_ACCESSION = "SRA accession";
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final BioSamplesClient webinClient;

  public RestIntegrationSRAV2(
      final BioSamplesClient client, @Qualifier("WEBINCLIENT") final BioSamplesClient webinClient) {
    super(client, webinClient);
    this.webinClient = webinClient;
  }

  @Override
  protected void phaseOne() {}

  @Override
  protected void phaseTwo() throws InterruptedException {}

  @Override
  protected void phaseThree() throws InterruptedException {}

  @Override
  protected void phaseFour() {}

  @Override
  protected void phaseFive() {}

  @Override
  protected void phaseSix() throws ExecutionException, InterruptedException {
    log.info("Starting SRA accession related tests");

    final Sample webinSampleTest1 = getWebinSampleTest1();
    final Sample webinSampleTest2 = getWebinSampleTest2();
    final Sample sample1Content = persistAndFetch(webinSampleTest1);

    if (sample1Content.getAttributes().stream()
        .noneMatch(attribute -> attribute.getType().equals(SRA_ACCESSION))) {
      throw new IntegrationTestFailException(
          "New sample doesn't contain a SRA accession attribute", Phase.SIX);
    }

    final Optional<Attribute> optionalSraAccessionAttributeForSampleTest1 =
        sample1Content.getAttributes().stream()
            .filter(attribute -> attribute.getType().equals(SRA_ACCESSION))
            .findFirst();

    if (optionalSraAccessionAttributeForSampleTest1.isEmpty()) {
      throw new IntegrationTestFailException(
          "New sample doesn't contain a SRA accession attribute", Phase.SIX);
    }

    final Attribute sraAccessionAttributeForSampleTest1 =
        optionalSraAccessionAttributeForSampleTest1.get();

    // re-post the first sample with accession, fetch and test
    final Sample sample1RePostContent = persistAndFetch(sample1Content);
    final Optional<Attribute> optionalSraAccessionAttributeForSampleTestAfterRePost =
        sample1RePostContent.getAttributes().stream()
            .filter(attribute -> attribute.getType().equals(SRA_ACCESSION))
            .findFirst();

    if (optionalSraAccessionAttributeForSampleTestAfterRePost.isEmpty()) {
      throw new IntegrationTestFailException(
          "New sample doesn't contain a SRA accession attribute", Phase.SIX);
    }

    final Attribute sraAccessionAttributeForSampleTest1AfterRePost =
        optionalSraAccessionAttributeForSampleTestAfterRePost.get();

    if (!sraAccessionAttributeForSampleTest1.equals(
        sraAccessionAttributeForSampleTest1AfterRePost)) {
      throw new IntegrationTestFailException(
          "SRA accession mismatch after sample post and re-post", Phase.SIX);
    }

    // submit sample 2 that already has a SRA accession
    // Submit with webin client, no jwt passed
    final Sample sample2Content = persistAndFetch(webinSampleTest2);

    if (sample2Content.getAttributes().stream()
        .noneMatch(attribute -> attribute.getType().equals(SRA_ACCESSION))) {
      throw new IntegrationTestFailException(
          "New sample-2 doesn't contain a SRA accession attribute", Phase.SIX);
    }

    final Optional<Attribute> optionalSraAccessionAttributeForSampleTest2 =
        sample2Content.getAttributes().stream()
            .filter(attribute -> attribute.getType().equals(SRA_ACCESSION))
            .findFirst();

    if (optionalSraAccessionAttributeForSampleTest2.isEmpty()) {
      throw new IntegrationTestFailException(
          "New sample-2 doesn't contain a SRA accession attribute", Phase.SIX);
    }

    final Attribute sraAccessionAttributeForSampleTest2 =
        optionalSraAccessionAttributeForSampleTest2.get();
    final Set<Attribute> sample2ContentAttributes = sample2Content.getAttributes();

    if (!sraAccessionAttributeForSampleTest2.equals(
        sample2ContentAttributes.stream()
            .filter(attribute -> attribute.getType().equals(SRA_ACCESSION))
            .findFirst()
            .get())) {
      throw new IntegrationTestFailException("New sample-2 SRA accession mismatch", Phase.SIX);
    }

    // remove SRA accession and add a different one
    sample2ContentAttributes.removeIf(attribute -> attribute.getType().equals(SRA_ACCESSION));
    sample2Content.getAttributes().add(Attribute.build(SRA_ACCESSION, "ERS100002"));

    try {
      webinClient.persistSampleResourceV2(Collections.singletonList(sample2Content));
    } catch (final Exception e) {
      log.info("Expectedly failed with message " + e.getMessage());
    }
  }

  private Sample persistAndFetch(final Sample sample) {
    // Submit with webin client, no jwt passed
    final List<Sample> samples =
        webinClient.persistSampleResourceV2(Collections.singletonList(sample));
    final String accession = Objects.requireNonNull(samples.get(0)).getAccession();
    final Optional<EntityModel<Sample>> fetchedSample = webinClient.fetchSampleResource(accession);

    return fetchedSample.get().getContent();
  }

  private Sample getWebinSampleTest1() {
    final String name = "RestIntegrationWebin_sample_1";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2116-04-01T11:36:57.00Z");

    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    attributes.add(Attribute.build("age", "3", null, Collections.emptyList(), "year"));
    attributes.add(Attribute.build("organism part", "lung"));
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
    final SortedSet<ExternalReference> externalReferences = new TreeSet<>();
    externalReferences.add(ExternalReference.build("http://www.google.com"));

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
            .name("Joe Bloggs")
            .role("Submitter")
            .email("jobloggs@joblogs.com")
            .build());

    final SortedSet<Publication> publications = new TreeSet<>();
    publications.add(
        new Publication.Builder().doi("10.1093/nar/gkt1081").pubmed_id("24265224").build());

    return new Sample.Builder(name)
        .withUpdate(update)
        .withRelease(release)
        .withWebinSubmissionAccountId("Webin-40894")
        .withAttributes(attributes)
        .withRelationships(relationships)
        .withExternalReferences(externalReferences)
        .withOrganizations(organizations)
        .withContacts(contacts)
        .withPublications(publications)
        .build();
  }

  private Sample getWebinSampleTest2() {
    final String name = "RestIntegrationWebin_sample_2";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2116-04-01T11:36:57.00Z");

    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    attributes.add(Attribute.build("age", "3", null, Collections.emptyList(), "year"));
    attributes.add(Attribute.build("organism part", "lung"));
    attributes.add(Attribute.build(SRA_ACCESSION, "ERS100001"));
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
    final SortedSet<ExternalReference> externalReferences = new TreeSet<>();
    externalReferences.add(ExternalReference.build("http://www.google.com"));

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
            .name("Joe Bloggs")
            .role("Submitter")
            .email("jobloggs@joblogs.com")
            .build());

    final SortedSet<Publication> publications = new TreeSet<>();
    publications.add(
        new Publication.Builder().doi("10.1093/nar/gkt1081").pubmed_id("24265224").build());

    return new Sample.Builder(name)
        .withUpdate(update)
        .withRelease(release)
        .withWebinSubmissionAccountId("Webin-40894")
        .withAttributes(attributes)
        .withRelationships(relationships)
        .withExternalReferences(externalReferences)
        .withOrganizations(organizations)
        .withContacts(contacts)
        .withPublications(publications)
        .build();
  }

  private Sample getWebinSampleMinimalInfo() {
    final String name = "RestIntegrationWebin_sample_1";

    return new Sample.Builder(name).build();
  }
}