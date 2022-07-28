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
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
@Order(1)
// @Profile({"default", "rest"})
public class RestSearchIntegration extends AbstractIntegration {
  public RestSearchIntegration(BioSamplesClient client) {
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
  protected void phaseTwo() {
    Sample test2 = getSampleTest2();
    Optional<Sample> optionalSample = fetchUniqueSampleByName(test2.getName());
    if (optionalSample.isPresent()) {
      test2 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + test2.getName(), Phase.TWO);
    }

    Sample test4 = getSampleTest4();
    optionalSample = fetchUniqueSampleByName(test4.getName());
    if (optionalSample.isPresent()) {
      test4 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + test4.getName(), Phase.TWO);
    }

    Sample test5 = getSampleTest5();
    optionalSample = fetchUniqueSampleByName(test5.getName());
    if (optionalSample.isPresent()) {
      test5 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + test5.getName(), Phase.TWO);
    }

    // post test4 again with relationships
    SortedSet<Relationship> relationships = new TreeSet<>();
    relationships.add(
        Relationship.build(test4.getAccession(), "derived from", test2.getAccession()));
    relationships.add(Relationship.build(test4.getAccession(), "derive to", test5.getAccession()));
    test4 = Sample.Builder.fromSample(test4).withRelationships(relationships).build();
    EntityModel<Sample> resource = client.persistSampleResource(test4);
    if (!test4.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response (" + resource.getContent() + ") to equal submission (" + test4 + ")",
          Phase.TWO);
    }

    // Build inverse relationships for sample5
    relationships = test5.getRelationships();
    relationships.add(Relationship.build(test4.getAccession(), "derive to", test5.getAccession()));
    test5 = Sample.Builder.fromSample(test5).withRelationships(relationships).build();
    Optional<EntityModel<Sample>> optionalResource =
        client.fetchSampleResource(test5.getAccession());
    if (optionalResource.isPresent()) {
      resource = optionalResource.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample not present, name: " + test5.getName(), Phase.TWO);
    }
    if (!test5.equals(resource.getContent())) {
      throw new IntegrationTestFailException(
          "Expected response (" + resource.getContent() + ") to equal submission (" + test5 + ")",
          Phase.TWO);
    }
  }

  @Override
  protected void phaseThree() {
    Sample test1 = getSampleTest1();
    Optional<Sample> optionalSample = fetchUniqueSampleByName(test1.getName());
    if (optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "Private sample in name search, sample name: " + test1.getName(), Phase.TWO);
    }

    Sample test2 = getSampleTest2();
    optionalSample = fetchUniqueSampleByName(test2.getName());
    if (optionalSample.isPresent()) {
      test2 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + test2.getName(), Phase.TWO);
    }

    List<EntityModel<Sample>> samples = new ArrayList<>();
    for (EntityModel<Sample> sample : publicClient.fetchSampleResourceAll()) {
      samples.add(sample);
    }

    if (samples.isEmpty()) {
      throw new IntegrationTestFailException("No search results found!", Phase.TWO);
    }

    // check that the private sample is not in search results
    // check that the referenced non-existing sample not in search result
    for (EntityModel<Sample> resource : publicClient.fetchSampleResourceAll()) {
      if (Objects.requireNonNull(resource.getContent()).getName().equals(test1.getName())) {
        throw new IntegrationTestFailException(
            "Found non-public sample " + test1.getAccession() + " in search samples", Phase.TWO);
      }
    }

    // TODO check OLS expansion by making sure we can find the submitted samples in results for
    // Eukaryota
    Set<String> accessions = new HashSet<>();
    for (EntityModel<Sample> sample : publicClient.fetchSampleResourceAll("Homo Sapiens")) {
      accessions.add(Objects.requireNonNull(sample.getContent()).getAccession());
    }
    if (!accessions.contains(test2.getAccession())) {
      throw new IntegrationTestFailException(
          test2.getAccession() + " not found in search results for Eukaryota", Phase.TWO);
    }
  }

  @Override
  protected void phaseFour() {
    Sample sample2 = getSampleTest2();
    Optional<Sample> optionalSample = fetchUniqueSampleByName(sample2.getName());
    if (optionalSample.isPresent()) {
      sample2 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + sample2.getName(), Phase.FOUR);
    }
    Sample sample4 = getSampleTest4();
    optionalSample = fetchUniqueSampleByName(sample4.getName());
    if (optionalSample.isPresent()) {
      sample4 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + sample4.getName(), Phase.FOUR);
    }
    Sample sample5 = getSampleTest5();
    optionalSample = fetchUniqueSampleByName(sample5.getName());
    if (optionalSample.isPresent()) {
      sample5 = optionalSample.get();
    } else {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + sample5.getName(), Phase.FOUR);
    }

    List<String> sample2ExpectedSearchResults =
        Arrays.asList(sample2.getAccession(), sample4.getAccession());
    List<String> sample4ExpectedSearchResults =
        Arrays.asList(sample2.getAccession(), sample4.getAccession(), sample5.getAccession());

    // Get results for sample2
    List<String> sample2EffectiveSearchResults = new ArrayList<>();
    for (EntityModel<Sample> sample : publicClient.fetchSampleResourceAll(sample2.getAccession())) {
      sample2EffectiveSearchResults.add(Objects.requireNonNull(sample.getContent()).getAccession());
    }

    if (sample2EffectiveSearchResults.isEmpty()) {
      throw new IntegrationTestFailException("No search results found!", Phase.FOUR);
    }

    if (!sample2EffectiveSearchResults.containsAll(sample2ExpectedSearchResults)) {
      throw new IntegrationTestFailException(
          "Search results for "
              + sample2.getAccession()
              + " does not contains all expected samples",
          Phase.FOUR);
    }

    // Get results for sample4
    List<String> sample4EffectiveSearchResults = new ArrayList<>();
    for (EntityModel<Sample> sample : publicClient.fetchSampleResourceAll(sample4.getAccession())) {
      sample4EffectiveSearchResults.add(Objects.requireNonNull(sample.getContent()).getAccession());
    }

    if (sample4EffectiveSearchResults.isEmpty()) {
      throw new IntegrationTestFailException("No search results found!", Phase.FOUR);
    }

    for (String expectedAccession : sample4ExpectedSearchResults) {
      if (!sample4EffectiveSearchResults.contains(expectedAccession)) {
        throw new RuntimeException(
            "Search results for "
                + sample4.getAccession()
                + " does not contains expected sample "
                + expectedAccession);
      }
    }
  }

  @Override
  protected void phaseFive() {
    // not doing anything here
  }

  @Override
  protected void phaseSix() {}

  private Sample getSampleTest1() {
    String name = "RestSearchIntegration_sample_1";
    Instant release = Instant.parse("2116-04-01T11:36:57.00Z");

    SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

    return new Sample.Builder(name)
        .withDomain(defaultIntegrationSubmissionDomain)
        .withRelease(release)
        .withAttributes(attributes)
        .build();
  }

  private Sample getSampleTest2() {
    String name = "RestSearchIntegration_sample_2_with_invalid_relationships";
    Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

    SortedSet<Relationship> relationships = new TreeSet<>();
    relationships.add(Relationship.build("SAMEA2", "derived from", "SAMEA3"));

    return new Sample.Builder(name)
        .withDomain(defaultIntegrationSubmissionDomain)
        .withRelease(release)
        .withRelationships(relationships)
        .withAttributes(attributes)
        .build();
  }

  private Sample getSampleTest4() {
    String name = "RestSearchIntegration_sample_4";
    Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

    return new Sample.Builder(name)
        .withDomain(defaultIntegrationSubmissionDomain)
        .withRelease(release)
        .withAttributes(attributes)
        .build();
  }

  private Sample getSampleTest5() {
    String name = "RestSearchIntegration_sample_5";
    Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

    return new Sample.Builder(name)
        .withDomain(defaultIntegrationSubmissionDomain)
        .withRelease(release)
        .withAttributes(attributes)
        .build();
  }
}
