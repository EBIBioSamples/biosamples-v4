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
import java.util.Collections;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.utils.ClientProperties;
import uk.ac.ebi.biosamples.core.model.*;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.core.service.FilterBuilder;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
// @Profile({"default", "rest"})
public class RestFilterIntegration extends AbstractIntegration {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ClientProperties clientProperties;

  public RestFilterIntegration(
      final BioSamplesClient client, final ClientProperties clientProperties) {
    super(client);

    this.clientProperties = clientProperties;
  }

  @Override
  protected void phaseOne() {
    Sample testSample1 = getTestSample1();
    Sample testSample2 = getTestSample2();
    Sample testSample3 = getTestSample3();

    final Optional<Sample> optionalSample = fetchUniqueSampleByName(testSample1.getName());

    if (optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "RestFilterIntegration test sample should not be available during phase 1", Phase.ONE);
    }

    EntityModel<Sample> resource = webinClient.persistSampleResource(testSample1);

    final Attribute sraAccessionAttribute1 =
        resource.getContent().getAttributes().stream()
            .filter(attribute -> attribute.getType().equals("SRA accession"))
            .findFirst()
            .get();
    testSample1.getAttributes().add(sraAccessionAttribute1);
    testSample1 =
        Sample.Builder.fromSample(testSample1)
            .withAccession(resource.getContent().getAccession())
            .build();

    if (!testSample1.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response ("
              + resource.getContent()
              + ") to equal submission ("
              + testSample1
              + ")",
          Phase.ONE);
    }

    resource = webinClient.persistSampleResource(testSample2);

    final Attribute sraAccessionAttribute2 =
        resource.getContent().getAttributes().stream()
            .filter(attribute -> attribute.getType().equals("SRA accession"))
            .findFirst()
            .get();

    testSample2.getAttributes().add(sraAccessionAttribute2);
    testSample2 =
        Sample.Builder.fromSample(testSample2)
            .withAccession(resource.getContent().getAccession())
            .build();

    if (!testSample2.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response ("
              + resource.getContent()
              + ") to equal submission ("
              + testSample2
              + ")",
          Phase.ONE);
    }

    resource = webinClient.persistSampleResource(testSample3);

    final Attribute sraAccessionAttribute3 =
        resource.getContent().getAttributes().stream()
            .filter(attribute -> attribute.getType().equals("SRA accession"))
            .findFirst()
            .get();

    testSample3.getAttributes().add(sraAccessionAttribute3);
    testSample3 =
        Sample.Builder.fromSample(testSample3)
            .withAccession(resource.getContent().getAccession())
            .build();

    if (!testSample3.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response ("
              + resource.getContent()
              + ") to equal submission ("
              + testSample3
              + ")",
          Phase.ONE);
    }
  }

  @Override
  protected void phaseTwo() throws InterruptedException {
    Sample testSample2 = getTestSample2();
    Sample testSample3 = getTestSample3();

    TimeUnit.SECONDS.sleep(2);

    Optional<Sample> optionalSample = fetchUniqueSampleByName(testSample2.getName());

    if (optionalSample.isPresent()) {
      testSample2 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + testSample2.getName(), Phase.TWO);
    }

    TimeUnit.SECONDS.sleep(2);
    optionalSample = fetchUniqueSampleByName(testSample3.getName());

    if (optionalSample.isPresent()) {
      testSample3 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + testSample2.getName(), Phase.TWO);
    }

    final SortedSet<Relationship> relations = new TreeSet<>();

    relations.add(
        Relationship.build(testSample3.getAccession(), "parent of", testSample2.getAccession()));
    testSample3 = Sample.Builder.fromSample(testSample3).withRelationships(relations).build();

    webinClient.persistSampleResource(testSample3);
  }

  @Override
  protected void phaseThree() {
    Sample testSample1 = getTestSample1();
    Sample testSample2 = getTestSample2();
    Optional<Sample> optionalSample = fetchUniqueSampleByName(testSample1.getName());

    if (optionalSample.isPresent()) {
      testSample1 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + testSample1.getName(), Phase.THREE);
    }

    optionalSample = fetchUniqueSampleByName(testSample2.getName());

    if (optionalSample.isPresent()) {
      testSample2 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + testSample2.getName(), Phase.THREE);
    }

    log.info("Getting sample 1 using filter on attribute");

    Filter attributeFilter =
        FilterBuilder.create().onAttribute("TestAttribute").withValue("FilterMe").build();
    PagedModel<EntityModel<Sample>> samplePage =
        webinClient.fetchPagedSampleResource("", Collections.singletonList(attributeFilter), 0, 10);

    if (samplePage.getMetadata().getTotalElements() != 1) {
      throw new IntegrationTestFailException(
          "Unexpected number of results for attribute filter query: "
              + samplePage.getMetadata().getTotalElements(),
          Phase.THREE);
    }

    EntityModel<Sample> restSample = samplePage.getContent().iterator().next();

    if (!restSample.getContent().equals(testSample1)) {
      throw new IntegrationTestFailException(
          "Unexpected number of results for attribute filter query: "
              + samplePage.getMetadata().getTotalElements(),
          Phase.THREE);
    }

    log.info("Getting sample 2 using filter on attribute with comma in the value");

    Optional<Attribute> targetAttribute =
        testSample1.getAttributes().stream()
            .filter(attribute -> attribute.getType().equalsIgnoreCase("description"))
            .findFirst();

    if (!targetAttribute.isPresent()) {
      throw new IntegrationTestFailException(
          "Sample 2 should contain an attribute description", Phase.THREE);
    }

    attributeFilter =
        FilterBuilder.create()
            .onAttribute(targetAttribute.get().getType())
            .withValue(targetAttribute.get().getValue())
            .build();

    samplePage =
        webinClient.fetchPagedSampleResource("", Collections.singletonList(attributeFilter), 0, 10);

    if (samplePage.getMetadata().getTotalElements() != 1) {
      throw new IntegrationTestFailException(
          "Unexpected number of results for attribute filter query: "
              + samplePage.getMetadata().getTotalElements(),
          Phase.THREE);
    }

    restSample = samplePage.getContent().iterator().next();

    if (!restSample.getContent().getAccession().equals(testSample1.getAccession())) {
      throw new IntegrationTestFailException(
          "Unexpected number of results for attribute filter query: "
              + samplePage.getMetadata().getTotalElements(),
          Phase.THREE);
    }

    log.info("Getting sample 2 using filter on attribute with regex which should not be picked up");

    targetAttribute =
        testSample1.getAttributes().stream()
            .filter(attribute -> attribute.getType().equalsIgnoreCase("Submission title"))
            .findFirst();

    if (!targetAttribute.isPresent()) {
      throw new IntegrationTestFailException(
          "Sample 2 should contain an attribute description", Phase.THREE);
    }

    attributeFilter =
        FilterBuilder.create().onAttribute(targetAttribute.get().getType()).withValue(null).build();
    samplePage =
        webinClient.fetchPagedSampleResource("", Collections.singletonList(attributeFilter), 0, 10);

    if (samplePage.getMetadata().getTotalElements() != 1) {
      throw new IntegrationTestFailException(
          "Unexpected number of results for attribute filter query: "
              + samplePage.getMetadata().getTotalElements(),
          Phase.THREE);
    }

    restSample = samplePage.getContent().iterator().next();

    if (!restSample.getContent().getAccession().equals(testSample1.getAccession())) {
      throw new IntegrationTestFailException(
          "Unexpected number of results for attribute filter query: "
              + samplePage.getMetadata().getTotalElements(),
          Phase.THREE);
    }

    log.info("Getting sample 2 using filter on attribute");

    attributeFilter =
        FilterBuilder.create().onAttribute("testAttribute").withValue("filterMe_1").build();
    samplePage =
        webinClient.fetchPagedSampleResource("", Collections.singletonList(attributeFilter), 0, 10);

    if (samplePage.getMetadata().getTotalElements() != 1) {
      throw new IntegrationTestFailException(
          "Unexpected number of results for attribute filter query: "
              + samplePage.getMetadata().getTotalElements(),
          Phase.THREE);
    }

    restSample = samplePage.getContent().iterator().next();

    if (!restSample.getContent().getAccession().equals(testSample2.getAccession())) {
      throw new IntegrationTestFailException(
          "Unexpected number of results for attribute filter query: "
              + samplePage.getMetadata().getTotalElements(),
          Phase.THREE);
    }

    log.info("Getting sample 2 using filter on name");

    final Filter nameFilter = FilterBuilder.create().onName(testSample2.getName()).build();

    samplePage =
        webinClient.fetchPagedSampleResource("", Collections.singletonList(nameFilter), 0, 10);

    if (samplePage.getMetadata().getTotalElements() != 1) {
      throw new IntegrationTestFailException(
          "Unexpected number of results for attribute filter query: "
              + samplePage.getMetadata().getTotalElements(),
          Phase.THREE);
    }

    restSample = samplePage.getContent().iterator().next();

    if (!restSample.getContent().getAccession().equals(testSample2.getAccession())) {
      throw new IntegrationTestFailException(
          "Unexpected number of results for attribute filter query: "
              + samplePage.getMetadata().getTotalElements(),
          Phase.THREE);
    }

    log.info("Getting sample 1 and 2 using filter on accession");

    final Filter accessionFilter =
        FilterBuilder.create().onAccession(testSample2.getAccession()).build();

    samplePage =
        webinClient.fetchPagedSampleResource("", Collections.singletonList(accessionFilter), 0, 10);

    if (samplePage.getMetadata().getTotalElements() != 1) {
      throw new IntegrationTestFailException(
          "Unexpected number of results for attribute filter query: "
              + samplePage.getMetadata().getTotalElements(),
          Phase.THREE);
    }

    final String accession1 = testSample1.getAccession();
    final String accession2 = testSample2.getAccession();

    if (!samplePage.getContent().stream()
        .allMatch(
            r ->
                r.getContent().getAccession().equals(accession1)
                    || r.getContent().getAccession().equals(accession2))) {
      throw new IntegrationTestFailException(
          "Unexpected number of results for attribute filter query: "
              + samplePage.getMetadata().getTotalElements(),
          Phase.THREE);
    }
  }

  @Override
  protected void phaseFour() {
    Sample testSample1 = getTestSample1();
    Sample testSample2 = getTestSample2();
    Sample testSample3 = getTestSample3();

    Optional<Sample> optionalSample = fetchUniqueSampleByName(testSample1.getName());

    if (optionalSample.isPresent()) {
      testSample1 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + testSample1.getName(), Phase.FOUR);
    }

    optionalSample = fetchUniqueSampleByName(testSample2.getName());

    if (optionalSample.isPresent()) {
      testSample2 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + testSample2.getName(), Phase.FOUR);
    }

    optionalSample = fetchUniqueSampleByName(testSample3.getName());

    if (optionalSample.isPresent()) {
      testSample3 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + testSample3.getName(), Phase.FOUR);
    }

    final String accession1 = testSample1.getAccession();
    final String accession2 = testSample2.getAccession();
    final String accession3 = testSample3.getAccession();

    log.info("Getting sample 1 using filter on date range");

    final Filter dateFilter =
        FilterBuilder.create()
            .onReleaseDate()
            .from(testSample1.getRelease().minusSeconds(2))
            .until(testSample1.getRelease().plusSeconds(2))
            .build();
    PagedModel<EntityModel<Sample>> samplePage =
        webinClient.fetchPagedSampleResource("", Collections.singletonList(dateFilter), 0, 10);

    if (samplePage.getMetadata().getTotalElements() < 1) {
      throw new IntegrationTestFailException(
          "Unexpected number of results for date range filter query: "
              + samplePage.getMetadata().getTotalElements());
    }

    boolean match =
        samplePage.getContent().stream()
            .anyMatch(resource -> resource.getContent().getAccession().equals(accession1));

    if (!match) {
      throw new IntegrationTestFailException(
          "Returned sample doesn't match the expected sample " + testSample1.getAccession(),
          Phase.FOUR);
    }

    log.info("Getting sample 3 using filter on relation");

    final Filter relFilter =
        FilterBuilder.create()
            .onRelation("parent of")
            .withValue(testSample2.getAccession())
            .build();
    samplePage =
        webinClient.fetchPagedSampleResource("", Collections.singletonList(relFilter), 0, 10);

    if (samplePage.getMetadata().getTotalElements() < 1) {
      throw new IntegrationTestFailException(
          "Unexpected number of results for relation filter query: "
              + samplePage.getMetadata().getTotalElements(),
          Phase.FOUR);
    }

    match =
        samplePage.getContent().stream()
            .anyMatch(resource -> resource.getContent().getAccession().equals(accession3));

    if (!match) {
      throw new IntegrationTestFailException(
          "Returned sample doesn't match the expected sample " + testSample3.getAccession(),
          Phase.FOUR);
    }

    log.info("Getting sample 2 using filter on inverse relation");

    final Filter invRelFilter =
        FilterBuilder.create()
            .onInverseRelation("parent of")
            .withValue(testSample3.getAccession())
            .build();
    samplePage =
        webinClient.fetchPagedSampleResource("", Collections.singletonList(invRelFilter), 0, 10);

    if (samplePage.getMetadata().getTotalElements() < 1) {
      throw new IntegrationTestFailException(
          "Unexpected number of results for relation filter query. Expected more than zero but got "
              + samplePage.getMetadata().getTotalElements());
    }

    match =
        samplePage.getContent().stream()
            .anyMatch(resource -> resource.getContent().getAccession().equals(accession2));

    if (!match) {
      throw new IntegrationTestFailException(
          "Returned sample doesn't match the expected sample " + testSample3.getAccession(),
          Phase.FOUR);
    }
  }

  @Override
  protected void phaseFive() {
    log.info("Getting results filtered by Webin Id");

    final Filter authFilter =
        FilterBuilder.create().onAuthInfo(defaultWebinIdForIntegrationTests).build();
    final PagedModel<EntityModel<Sample>> samplePage =
        webinClient.fetchPagedSampleResource("", Collections.singletonList(authFilter), 0, 10);

    if (samplePage.getMetadata().getTotalElements() < 1) {
      throw new IntegrationTestFailException(
          "Unexpected number of results for domain filter query: "
              + samplePage.getMetadata().getTotalElements(),
          Phase.FIVE);
    }
  }

  @Override
  protected void phaseSix() {}

  private Sample getTestSample1() {
    final String name = "RestFilterIntegration_sample_1";
    final Instant update = Instant.parse("1999-12-25T11:36:57.00Z");
    final Instant release = Instant.parse("1999-12-25T11:36:57.00Z");
    final SortedSet<Attribute> attributes = new TreeSet<>();

    attributes.add(Attribute.build("TestAttribute", "FilterMe"));
    attributes.add(
        Attribute.build(
            "description",
            "Sequencing of barley BACs, that constitute the MTP of chromosome 7H. Sequencing was carried out by BGI China.8439"));
    attributes.add(
        Attribute.build(
            "Submission title", "Regular Expression * risky (I know what I'm saying) [0-9]+?"));
    attributes.add(Attribute.build("Organism", "Human"));

    return new Sample.Builder(name)
        .withWebinSubmissionAccountId(clientProperties.getBiosamplesClientWebinUsername())
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(attributes)
        .build();
  }

  private Sample getTestSample2() {
    final String name = "RestFilterIntegration_sample_2";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");
    final SortedSet<Attribute> attributes = new TreeSet<>();

    attributes.add(
        Attribute.build(
            "testAttribute", "filterMe_1", "http://www.ebi.ac.uk/efo/EFO_0001071", null));
    attributes.add(Attribute.build("Organism", "Human"));

    return new Sample.Builder(name)
        .withWebinSubmissionAccountId(clientProperties.getBiosamplesClientWebinUsername())
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(attributes)
        .build();
  }

  private Sample getTestSample3() {
    final String name = "RestFilterIntegration_sample_3";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    return new Sample.Builder(name)
        .withWebinSubmissionAccountId(clientProperties.getBiosamplesClientWebinUsername())
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(Collections.singleton(Attribute.build("Organism", "Human")))
        .build();
  }
}
