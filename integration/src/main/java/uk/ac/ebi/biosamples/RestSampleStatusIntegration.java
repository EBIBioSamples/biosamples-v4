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
import javax.annotation.PreDestroy;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.utils.ClientProperties;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SampleStatus;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
@Order(2)
public class RestSampleStatusIntegration extends AbstractIntegration {
  private final BioSamplesClient anonymousClient;
  private final ClientProperties clientProperties;

  public RestSampleStatusIntegration(
      final BioSamplesClient client,
      final RestTemplateBuilder restTemplateBuilder,
      final ClientProperties clientProperties) {
    super(client);

    this.clientProperties = clientProperties;
    anonymousClient =
        new BioSamplesClient(
            clientProperties.getBiosamplesClientUri(),
            clientProperties.getBiosamplesClientUriV2(),
            restTemplateBuilder,
            null,
            null,
            clientProperties);
  }

  @Override
  protected void phaseOne() {
    final Sample publicSample = getPublicSample();
    final Sample suppressedSample = getSuppressedSample();
    final Sample privateSample = getPrivateSample();
    final Sample publicSampleWithPrivateStatus = getPublicSampleWithWrongStatus();

    fetchByNameAndThrowOrElsePersist(publicSample);
    fetchByNameAndThrowOrElsePersist(suppressedSample);
    fetchByNameAndThrowOrElsePersist(privateSample);
    fetchByNameAndThrowOrElsePersist(publicSampleWithPrivateStatus);
  }

  @Override
  protected void phaseTwo() {
    final Sample publicSample = getPublicSample();
    final Sample suppressedSample = getSuppressedSample();
    final Sample privateSample = getPrivateSample();
    final Sample publicSampleWithPrivateStatus = getPublicSampleWithWrongStatus();

    final Optional<Sample> publicSampleDb = fetchUniqueSampleByName(publicSample.getName());
    final Optional<Sample> suppressedSampleDb = fetchUniqueSampleByName(suppressedSample.getName());
    final Optional<Sample> privateSampleDb = fetchUniqueSampleByName(privateSample.getName());
    final Optional<Sample> publicSampleWithPrivateStatusDb =
        fetchUniqueSampleByName(publicSampleWithPrivateStatus.getName());

    publicSampleDb.orElseThrow(
        () ->
            new IntegrationTestFailException(
                "PUBLIC sample not present in search result: " + publicSample.getName(),
                Phase.TWO));
    suppressedSampleDb.ifPresent(
        s -> {
          throw new IntegrationTestFailException(
              "SUPPRESSED sample present in search result: " + suppressedSample.getName(),
              Phase.TWO);
        });
    privateSampleDb.ifPresent(
        s -> {
          throw new IntegrationTestFailException(
              "PRIVATE sample present in search result: " + privateSample.getName(), Phase.TWO);
        });
    publicSampleWithPrivateStatusDb.orElseThrow(
        () ->
            new IntegrationTestFailException(
                "PUBLIC sample not present in search result: "
                    + publicSampleWithPrivateStatus.getName(),
                Phase.TWO));
  }

  @Override
  protected void phaseThree() throws InterruptedException {}

  @Override
  protected void phaseFour() {}

  @Override
  protected void phaseFive() {}

  @Override
  protected void phaseSix() {}

  private Sample getPublicSample() {
    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    attributes.add(Attribute.build("age", "3", null, Collections.emptyList(), "year"));
    attributes.add(Attribute.build("organism part", "lung"));
    attributes.add(Attribute.build("sex", "female"));

    return new Sample.Builder("RestSampleStatusIntegration_sample_public")
        .withTaxId(9606L)
        .withStatus(SampleStatus.PUBLIC)
        .withUpdate(Instant.parse("2016-05-05T11:36:57.00Z"))
        .withRelease(Instant.parse("2016-04-01T11:36:57.00Z"))
        .withWebinSubmissionAccountId(clientProperties.getBiosamplesClientWebinUsername())
        .withAttributes(attributes)
        .build();
  }

  private Sample getSuppressedSample() {
    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    attributes.add(Attribute.build("age", "3", null, Collections.emptyList(), "year"));
    attributes.add(Attribute.build("organism part", "lung"));
    attributes.add(Attribute.build("sex", "female"));
    attributes.add(Attribute.build("INSDC status", "suppressed"));

    return new Sample.Builder("RestSampleStatusIntegration_sample_suppressed")
        .withTaxId(9606L)
        .withStatus(SampleStatus.SUPPRESSED)
        .withUpdate(Instant.parse("2016-05-05T11:36:57.00Z"))
        .withRelease(Instant.parse("2016-04-01T11:36:57.00Z"))
        .withWebinSubmissionAccountId(clientProperties.getBiosamplesClientWebinUsername())
        .withAttributes(attributes)
        .build();
  }

  private Sample getPrivateSample() {
    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    attributes.add(Attribute.build("age", "3", null, Collections.emptyList(), "year"));
    attributes.add(Attribute.build("organism part", "lung"));
    attributes.add(Attribute.build("sex", "female"));

    return new Sample.Builder("RestSampleStatusIntegration_sample_private")
        .withTaxId(9606L)
        .withStatus(SampleStatus.PRIVATE)
        .withUpdate(Instant.parse("2016-05-05T11:36:57.00Z"))
        .withRelease(Instant.parse("2116-04-01T11:36:57.00Z"))
        .withWebinSubmissionAccountId(clientProperties.getBiosamplesClientWebinUsername())
        .withAttributes(attributes)
        .build();
  }

  private Sample getPublicSampleWithWrongStatus() {
    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    attributes.add(Attribute.build("age", "3", null, Collections.emptyList(), "year"));
    attributes.add(Attribute.build("organism part", "lung"));
    attributes.add(Attribute.build("sex", "female"));

    return new Sample.Builder("RestSampleStatusIntegration_sample_public_with_wrong_status")
        .withTaxId(9606L)
        .withStatus(SampleStatus.PRIVATE)
        .withUpdate(Instant.parse("2016-05-05T11:36:57.00Z"))
        .withRelease(Instant.parse("2016-04-01T11:36:57.00Z"))
        .withWebinSubmissionAccountId(clientProperties.getBiosamplesClientWebinUsername())
        .withAttributes(attributes)
        .build();
  }

  @PreDestroy
  public void destroy() {
    anonymousClient.close();
  }
}
