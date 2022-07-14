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
import java.util.concurrent.TimeUnit;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
public class RestStaticViewIntegration extends AbstractIntegration {

  public RestStaticViewIntegration(BioSamplesClient client) {
    super(client);
  }

  @Override
  protected void phaseOne() {
    Sample test1 = getSampleTest1();
    Sample test2 = getSampleTest2();
    Sample test4 = getSampleTest4();
    Sample test5 = getSampleTest5();

    // put a private sample
    EntityModel<Sample> resource = client.persistSampleResource(test1);
    test1 =
        Sample.Builder.fromSample(test1)
            .withAccession(Objects.requireNonNull(resource.getContent()).getAccession())
            .build();
    if (!test1.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response (" + resource.getContent() + ") to equal submission (" + test1 + ")",
          Phase.ONE);
    }

    // put a sample that refers to a non-existing sample, Should this fail???
    resource = client.persistSampleResource(test2);
    test2 =
        Sample.Builder.fromSample(test2)
            .withAccession(Objects.requireNonNull(resource.getContent()).getAccession())
            .build();
    if (!test2.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response (" + resource.getContent() + ") to equal submission (" + test2 + ")",
          Phase.ONE);
    }

    resource = client.persistSampleResource(test4);
    test4 =
        Sample.Builder.fromSample(test4)
            .withAccession(Objects.requireNonNull(resource.getContent()).getAccession())
            .build();
    if (!test4.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response (" + resource.getContent() + ") to equal submission (" + test4 + ")",
          Phase.ONE);
    }

    resource = client.persistSampleResource(test5);
    test5 =
        Sample.Builder.fromSample(test5)
            .withAccession(Objects.requireNonNull(resource.getContent()).getAccession())
            .build();
    if (!test5.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response (" + resource.getContent() + ") to equal submission (" + test5 + ")",
          Phase.ONE);
    }
  }

  @Override
  protected void phaseTwo() throws InterruptedException {
    Sample test2 = getSampleTest2();
    Sample test4 = getSampleTest4();
    Sample test5 = getSampleTest5();

    TimeUnit.SECONDS.sleep(2);
    Optional<Sample> optionalSample = fetchUniqueSampleByName(test2.getName());
    if (optionalSample.isPresent()) {
      test2 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + test2.getName(), Phase.TWO);
    }

    optionalSample = fetchUniqueSampleByName(test4.getName());
    if (optionalSample.isPresent()) {
      test4 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + test4.getName(), Phase.TWO);
    }

    optionalSample = fetchUniqueSampleByName(test5.getName());
    if (optionalSample.isPresent()) {
      test5 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + test5.getName(), Phase.TWO);
    }

    Set<Attribute> attributesPre;
    Set<Attribute> attributesPost;

    attributesPre = new HashSet<>();
    attributesPre.add(Attribute.build("organism", "9606"));
    attributesPost = new HashSet<>();
    attributesPost.add(Attribute.build("organism", "Homo sapiens"));
    client.persistCuration(
        test2.getAccession(),
        Curation.build(attributesPre, attributesPost, null, null),
        defaultIntegrationSubmissionDomain,
        false);

    attributesPre = new HashSet<>();
    attributesPre.add(Attribute.build("organism", "Homo sapiens"));
    attributesPost = new HashSet<>();
    attributesPost.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    client.persistCuration(
        test2.getAccession(),
        Curation.build(attributesPre, attributesPost, null, null),
        defaultIntegrationSubmissionDomain,
        false);

    SortedSet<Relationship> relationships = new TreeSet<>();
    relationships.add(
        Relationship.build(test4.getAccession(), "derived from", test2.getAccession()));
    relationships.add(Relationship.build(test4.getAccession(), "derive to", test5.getAccession()));
    Sample sample4WithRelationships =
        Sample.Builder.fromSample(test4).withRelationships(relationships).build();
    EntityModel<Sample> resource = client.persistSampleResource(sample4WithRelationships);
    if (!sample4WithRelationships.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response ("
              + resource.getContent()
              + ") to equal submission ("
              + sample4WithRelationships
              + ")",
          Phase.TWO);
    }

    optionalSample = fetchUniqueSampleByName(test5.getName());
    if (!optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + test5.getName(), Phase.TWO);
    }

    // Build inverse relationships for sample5
    SortedSet<Relationship> test5AllRelationships = test5.getRelationships();
    test5AllRelationships.add(
        Relationship.build(test4.getAccession(), "derive to", test5.getAccession()));
    test5 = Sample.Builder.fromSample(test5).withRelationships(test5AllRelationships).build();
    if (!test5.equals(optionalSample.get())) {
      throw new IntegrationTestFailException(
          "Expected response (" + resource.getContent() + ") to equal submission (" + test5 + ")",
          Phase.TWO);
    }

    relationships = new TreeSet<>();
    relationships.add(
        Relationship.build(test5.getAccession(), "derived from", test2.getAccession()));
    Sample sample5WithRelationships =
        Sample.Builder.fromSample(test5).withRelationships(relationships).build();
    resource = client.persistSampleResource(sample5WithRelationships);

    Sample sample5WithInverseRelationships =
        Sample.Builder.fromSample(sample5WithRelationships)
            .addRelationship(
                Relationship.build(test4.getAccession(), "derive to", test5.getAccession()))
            .build();

    if (!sample5WithInverseRelationships.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response ("
              + resource.getContent()
              + ") to equal submission ("
              + sample5WithRelationships
              + ")",
          Phase.TWO);
    }
  }

  @Override
  protected void phaseThree() {
    Sample test1 = getSampleTest1();
    Sample test2 = getSampleTest2();
    Sample test4 = getSampleTest4();
    Sample test5 = getSampleTest5();

    Optional<Sample> optionalSample = fetchUniqueSampleByName(test2.getName());
    if (optionalSample.isPresent()) {
      test2 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + test2.getName(), Phase.THREE);
    }

    optionalSample = fetchUniqueSampleByName(test4.getName());
    if (optionalSample.isPresent()) {
      test4 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + test4.getName(), Phase.THREE);
    }

    optionalSample = fetchUniqueSampleByName(test5.getName());
    if (optionalSample.isPresent()) {
      test5 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + test5.getName(), Phase.THREE);
    }

    List<EntityModel<Sample>> samples = new ArrayList<>();
    for (EntityModel<Sample> sample : client.fetchSampleResourceAll()) {
      samples.add(sample);
    }

    if (samples.isEmpty()) {
      throw new IntegrationTestFailException("No search results found!", Phase.THREE);
    }

    // check that the private sample is not in search results
    // check that the referenced non-existing sample not in search result
    for (EntityModel<Sample> resource : client.fetchSampleResourceAll()) {
      if (resource.getContent().getAccession().equals(test1.getName())) {
        throw new IntegrationTestFailException(
            "Found non-public sample " + test1.getName() + " in search samples", Phase.THREE);
      }
    }

    testDynamicAndStaticView(test2.getAccession());
    testDynamicAndStaticView(test4.getAccession());
    testDynamicAndStaticView(test5.getAccession());

    test4 = Sample.Builder.fromSample(getSampleTest4()).withAccession(test4.getAccession()).build();
    // delete relationships again
    EntityModel<Sample> resource = client.persistSampleResource(test4);
    if (!test4.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response (" + resource.getContent() + ") to equal submission (" + test4 + ")");
    }
  }

  @Override
  protected void phaseFour() {
    Sample test4 = getSampleTest4();
    Optional<Sample> optionalSample = fetchUniqueSampleByName(test4.getName());
    if (optionalSample.isPresent()) {
      test4 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + test4.getName(), Phase.THREE);
    }
    testDynamicAndStaticView(test4.getAccession());
  }

  @Override
  protected void phaseFive() {
    // nothing to do here
  }

  @Override
  protected void phaseSix() {}

  private void testDynamicAndStaticView(String accession) {
    Sample dynamicSample;
    Optional<EntityModel<Sample>> optionalSample =
        client.fetchSampleResource(
            accession, Optional.empty(), null, StaticViewWrapper.StaticView.SAMPLES_DYNAMIC);
    if (optionalSample.isPresent()) {
      dynamicSample = optionalSample.get().getContent();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample accession: " + accession);
    }

    Sample staticSample;
    optionalSample =
        client.fetchSampleResource(
            accession, Optional.empty(), null, StaticViewWrapper.StaticView.SAMPLES_CURATED);
    if (optionalSample.isPresent()) {
      staticSample = optionalSample.get().getContent();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample accession: " + accession);
    }

    Sample sample;
    optionalSample = client.fetchSampleResource(accession);
    if (optionalSample.isPresent()) {
      sample = optionalSample.get().getContent();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample accession: " + accession);
    }

    if (!dynamicSample.equals(staticSample)) {
      throw new IntegrationTestFailException(
          "Expected response (" + dynamicSample + ") to equal submission (" + staticSample + ")");
    }

    if (!dynamicSample.equals(sample)) {
      throw new IntegrationTestFailException(
          "Expected response (" + dynamicSample + ") to equal submission (" + sample + ")");
    }
  }

  private Sample getSampleTest1() {
    String name = "RestStaticViewIntegration_sample_1";
    Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    Instant release = Instant.parse("2116-04-01T11:36:57.00Z");

    SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

    return new Sample.Builder(name)
        .withDomain(defaultIntegrationSubmissionDomain)
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(attributes)
        .build();
  }

  private Sample getSampleTest2() {
    String name = "RestStaticViewIntegration_sample_2";
    Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(Attribute.build("organism", "9606"));

    SortedSet<Relationship> relationships = new TreeSet<>();
    relationships.add(Relationship.build("SAMEA648208572", "derived from", "SAMEA648208573"));

    return new Sample.Builder(name)
        .withDomain(defaultIntegrationSubmissionDomain)
        .withRelease(release)
        .withUpdate(update)
        .withRelationships(relationships)
        .withAttributes(attributes)
        .build();
  }

  private Sample getSampleTest4() {
    String name = "RestStaticViewIntegration_sample_4";
    Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

    return new Sample.Builder(name)
        .withDomain(defaultIntegrationSubmissionDomain)
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(attributes)
        .build();
  }

  private Sample getSampleTest5() {
    String name = "RestStaticViewIntegration_sample_5";
    Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

    return new Sample.Builder(name)
        .withDomain(defaultIntegrationSubmissionDomain)
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(attributes)
        .build();
  }
}
