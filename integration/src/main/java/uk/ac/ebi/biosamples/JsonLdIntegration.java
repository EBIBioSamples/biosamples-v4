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
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.utils.ClientProperties;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JsonLdIntegration extends AbstractIntegration {
  private final Environment env;
  private final RestOperations restTemplate;
  private final ClientProperties clientProperties;

  public JsonLdIntegration(
      final RestTemplateBuilder templateBuilder,
      final BioSamplesClient client,
      final ClientProperties clientProperties,
      final Environment env) {
    super(client);
    restTemplate = templateBuilder.build();
    this.env = env;
    this.clientProperties = clientProperties;
  }

  @Override
  protected void phaseOne() {
    final Sample testSample = getTestSample();
    final Optional<Sample> optionalSample = fetchUniqueSampleByName(testSample.getName());

    if (optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "JsonLD test sample should not be available during phase 1", Phase.ONE);
    } else {
      final EntityModel<Sample> resource = client.persistSampleResource(testSample);
      final Sample sampleContent = resource.getContent();
      final Attribute sraAccessionAttribute =
          sampleContent.getAttributes().stream()
              .filter(attribute -> attribute.getType().equals("SRA accession"))
              .findFirst()
              .get();

      testSample.getAttributes().add(sraAccessionAttribute);

      final Sample testSampleWithAccession =
          Sample.Builder.fromSample(testSample)
              .withAccession(Objects.requireNonNull(sampleContent).getAccession())
              .withSraAccession(Objects.requireNonNull(sampleContent).getSraAccession())
              .withStatus(sampleContent.getStatus())
              .build();

      if (!sampleContent.equals(testSampleWithAccession)) {
        throw new IntegrationTestFailException(
            "Expected response ("
                + sampleContent
                + ") to equal submission ("
                + testSampleWithAccession
                + ")");
      }
    }
  }

  @Override
  protected void phaseTwo() {
    final Sample testSample = getTestSample();
    // Check if selenium profile is activate
    if (isSeleniumTestRequired(env)) {
      // checkPresenceOnWebPage(testSample);
    }
    try {
      checkPresenceWithRest(testSample.getName());
    } catch (final InterruptedException e) {
      throw new IntegrationTestFailException(
          "JsonLD test sample not present in db despite waiting for 2 seconds");
    }
  }

  @Override
  protected void phaseThree() {}

  @Override
  protected void phaseFour() {}

  @Override
  protected void phaseFive() {}

  @Override
  protected void phaseSix() {}

  private boolean jsonLDIsEmpty(final String jsonLDContent) {
    return jsonLDContent.matches("\\{\\s+}");
  }

  private Sample getTestSample() {
    final String name = "JsonLdIntegration_sample_1";
    final Instant update = Instant.parse("2016-05-05T11:36:57.00Z");
    final Instant release = Instant.parse("2016-04-01T11:36:57.00Z");
    final SortedSet<Attribute> attributes = new TreeSet<>();

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

    final SortedSet<ExternalReference> externalReferences = new TreeSet<>();

    externalReferences.add(
        ExternalReference.build(
            "www.google.com",
            new TreeSet<>(Arrays.asList("DUO:0000005", "DUO:0000001", "DUO:0000007"))));

    return new Sample.Builder(name)
        .withTaxId(Long.valueOf(9606))
        .withWebinSubmissionAccountId(clientProperties.getBiosamplesClientWebinUsername())
        .withRelease(release)
        .withUpdate(update)
        .withAttributes(attributes)
        .withExternalReferences(externalReferences)
        .build();
  }

  private boolean jsonLDHasAccession(final String jsonLDContent, final String accession) {
    return Pattern.compile("\"identifier\"\\s*:\\s*\"" + accession + "\",")
        .matcher(jsonLDContent)
        .find();
  }

  private void checkPresenceWithRest(final String sampleName) throws InterruptedException {
    final Sample sample;
    TimeUnit.SECONDS.sleep(2);

    final Optional<Sample> optionalSample = fetchUniqueSampleByName(sampleName);
    if (optionalSample.isPresent()) {
      sample = optionalSample.get();
    } else {
      throw new IntegrationTestFailException("JsonLD test sample not present in db");
    }

    final UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromUri(clientProperties.getBiosamplesClientUri());
    uriBuilder.pathSegment("samples", sample.getAccession() + ".ldjson");
    final ResponseEntity<JsonLDDataRecord> responseEntity =
        restTemplate.getForEntity(uriBuilder.toUriString(), JsonLDDataRecord.class);

    if (!responseEntity.getStatusCode().is2xxSuccessful()) {
      throw new RuntimeException(
          "Error retrieving sample in application/ld+json format from the webapp");
    }

    final JsonLDDataRecord jsonLDDataRecord = responseEntity.getBody();
    final JsonLDSample jsonLDSample = jsonLDDataRecord.getMainEntity();

    assert Stream.of(jsonLDSample.getIdentifiers())
        .anyMatch(s -> s.equals("biosamples:" + sample.getAttributes()));

    final String checkingUrl =
        UriComponentsBuilder.fromUri(clientProperties.getBiosamplesClientUri())
            .pathSegment("samples", sample.getAccession())
            .toUriString();

    assert jsonLDSample.getUrl().equals(checkingUrl);
  }

  private boolean isSeleniumTestRequired(final Environment env) {
    return Stream.of(env.getActiveProfiles()).anyMatch(value -> value.matches("selenium"));
  }
}
