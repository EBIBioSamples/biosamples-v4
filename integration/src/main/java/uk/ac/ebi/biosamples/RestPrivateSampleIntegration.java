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
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.utils.ClientProperties;
import uk.ac.ebi.biosamples.core.model.*;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
public class RestPrivateSampleIntegration extends AbstractIntegration {
  private final ClientProperties clientProperties;

  public RestPrivateSampleIntegration(
      final BioSamplesClient client, final ClientProperties clientProperties) {
    super(client);

    this.clientProperties = clientProperties;
  }

  @Override
  protected void phaseOne() {
    final Sample publicSampleToday = getSampleWithReleaseDateToday();
    final Sample privateSample = getSampleWithFutureReleaseDate();
    Optional<Sample> optionalSample = fetchUniqueSampleByName(publicSampleToday.getName());

    if (optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "RestPrivateSampleIntegration test sample should not be available during phase 1",
          Phase.ONE);
    }

    webinClient.persistSampleResource(publicSampleToday);

    optionalSample = fetchUniqueSampleByName(privateSample.getName());

    if (optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "RestPrivateSampleIntegration test sample should not be available during phase 1",
          Phase.ONE);
    }

    webinClient.persistSampleResource(privateSample);
  }

  @Override
  protected void phaseTwo() {
    final Sample publicSampleToday = getSampleWithReleaseDateToday();
    final Sample privateSample = getSampleWithFutureReleaseDate();
    Optional<Sample> optionalSample = fetchUniqueSampleByName(publicSampleToday.getName());

    if (optionalSample.isEmpty()) {
      throw new IntegrationTestFailException(
          "Sample does not exist, sample name: " + publicSampleToday.getName(), Phase.TWO);
    }

    optionalSample = fetchUniqueSampleByName(privateSample.getName());

    if (optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "Private sample exists, sample name: " + privateSample.getName(), Phase.TWO);
    }
  }

  @Override
  protected void phaseThree() {
    // nothing to do here
  }

  @Override
  protected void phaseFour() {
    // nothing to do here
  }

  @Override
  protected void phaseFive() {
    // nothing to do here
  }

  @Override
  protected void phaseSix() {}

  private Sample getSampleWithReleaseDateToday() {
    final String name = "RestPrivateSampleIntegration_sample_1";
    final Instant release = Instant.now();
    final SortedSet<Attribute> attributes = new TreeSet<>();

    attributes.add(Attribute.build("description", "Fake sample with today(now) release date"));
    attributes.add(Attribute.build("Organism", "Human"));

    return new Sample.Builder(name)
        .withWebinSubmissionAccountId(clientProperties.getBiosamplesClientWebinUsername())
        .withRelease(release)
        .withAttributes(attributes)
        .build();
  }

  private Sample getSampleWithFutureReleaseDate() {
    final String name = "RestPrivateSampleIntegration_sample_2";
    final Instant release = Instant.now().plusSeconds(3600);
    final SortedSet<Attribute> attributes = new TreeSet<>();

    attributes.add(Attribute.build("description", "Fake sample with future release date"));
    attributes.add(Attribute.build("Organism", "Human"));

    return new Sample.Builder(name)
        .withWebinSubmissionAccountId(clientProperties.getBiosamplesClientWebinUsername())
        .withRelease(release)
        .withAttributes(attributes)
        .build();
  }
}
