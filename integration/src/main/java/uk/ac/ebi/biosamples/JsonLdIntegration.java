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

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JsonLdIntegration extends AbstractIntegration {
  private final Environment env;
  private final RestOperations restTemplate;
  private final IntegrationProperties integrationProperties;
  private final BioSamplesProperties bioSamplesProperties;

  public JsonLdIntegration(
      RestTemplateBuilder templateBuilder,
      BioSamplesClient client,
      IntegrationProperties integrationProperties,
      BioSamplesProperties bioSamplesProperties,
      Environment env) {
    super(client);
    this.integrationProperties = integrationProperties;
    this.restTemplate = templateBuilder.build();
    this.env = env;
    this.bioSamplesProperties = bioSamplesProperties;
  }

  @Override
  protected void phaseOne() {
    Sample testSample = getTestSample();
    Optional<Sample> optionalSample = fetchUniqueSampleByName(testSample.getName());

    if (optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "JsonLD test sample should not be available during phase 1", Phase.ONE);
    } else {
      Resource<Sample> resource = client.persistSampleResource(testSample);
      Sample testSampleWithAccession =
          Sample.Builder.fromSample(testSample)
              .withAccession(resource.getContent().getAccession())
              .build();

      if (!testSampleWithAccession.equals(resource.getContent())) {
        throw new IntegrationTestFailException(
            "Expected response ("
                + resource.getContent()
                + ") to equal submission ("
                + testSample
                + ")");
      }
    }
  }

  @Override
  protected void phaseTwo() {
    Sample testSample = getTestSample();
    // Check if selenium profile is activate
    if (isSeleniumTestRequired(env)) {
      //            checkPresenceOnWebPage(testSample);
    }
    checkPresenceWithRest(testSample.getName());
  }

  @Override
  protected void phaseThree() {}

  @Override
  protected void phaseFour() {}

  @Override
  protected void phaseFive() {}

  private boolean jsonLDIsEmpty(String jsonLDContent) {
    return jsonLDContent.matches("\\{\\s+}");
  }

  private Sample getTestSample() {
    String name = "JsonLdIntegration_sample_1";
    Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    Instant release = Instant.parse("2016-04-01T11:36:57.00Z");

    SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(Attribute.build("organism", "Homo Sapiens"));
    attributes.add(
        Attribute.build(
            "Organism Part", "Lung", "http://purl.obolibrary.org/obo/UBERON_0002048", null));
    attributes.add(Attribute.build("test_Type", "test_value"));
    attributes.add(Attribute.build("Description", "Test description"));
    attributes.add(
        Attribute.build(
            "MultiCategoryCodeField",
            "heart and lung",
            null,
            Arrays.asList(
                "http://purl.obolibrary.org/obo/UBERON_0002048",
                "http://purl.obolibrary.org/obo/UBERON_0002045",
                "UBERON:0002045"),
            null));

    SortedSet<ExternalReference> externalReferences = new TreeSet<>();
    externalReferences.add(
        ExternalReference.build(
            "www.google.com",
            new TreeSet<>(Arrays.asList("DUO:0000005", "DUO:0000001", "DUO:0000007"))));

    return new Sample.Builder(name)
        .withDomain(defaultIntegrationSubmissionDomain)
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(attributes)
        .withExternalReferences(externalReferences)
        .build();
  }

  private boolean jsonLDHasAccession(String jsonLDContent, String accession) {
    return Pattern.compile("\"identifier\"\\s*:\\s*\"" + accession + "\",")
        .matcher(jsonLDContent)
        .find();
  }

  private void checkPresenceWithRest(String sampleName) {
    Sample sample;
    Optional<Sample> optionalSample = fetchUniqueSampleByName(sampleName);
    if (optionalSample.isPresent()) {
      sample = optionalSample.get();
    } else {
      throw new IntegrationTestFailException("JsonLD test sample not present in db");
    }

    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromUri(bioSamplesProperties.getBiosamplesClientUri());
    uriBuilder.pathSegment("samples", sample.getAccession() + ".ldjson");
    ResponseEntity<JsonLDDataRecord> responseEntity =
        restTemplate.getForEntity(uriBuilder.toUriString(), JsonLDDataRecord.class);
    if (!responseEntity.getStatusCode().is2xxSuccessful()) {
      throw new RuntimeException(
          "Error retrieving sample in application/ld+json format from the webapp");
    }
    JsonLDDataRecord jsonLDDataRecord = responseEntity.getBody();
    JsonLDSample jsonLDSample = jsonLDDataRecord.getMainEntity();
    assert Stream.of(jsonLDSample.getIdentifiers())
        .anyMatch(s -> s.equals("biosamples:" + sample.getAttributes()));

    String checkingUrl =
        UriComponentsBuilder.fromUri(bioSamplesProperties.getBiosamplesClientUri())
            .pathSegment("samples", sample.getAccession())
            .toUriString();
    assert jsonLDSample.getUrl().equals(checkingUrl);
  }

  private boolean isSeleniumTestRequired(Environment env) {
    return Stream.of(env.getActiveProfiles()).anyMatch(value -> value.matches("selenium"));
  }
}
