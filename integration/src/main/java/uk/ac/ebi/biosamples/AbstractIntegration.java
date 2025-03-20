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

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.hateoas.EntityModel;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

public abstract class AbstractIntegration implements ApplicationRunner, ExitCodeGenerator {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private int exitCode = 1; // don't make this final
  protected static final String defaultWebinIdForIntegrationTests = "Webin-40894";
  protected final BioSamplesClient noAuthClient;
  protected final BioSamplesClient webinClient;

  protected abstract void phaseOne();

  protected abstract void phaseTwo() throws InterruptedException;

  protected abstract void phaseThree() throws InterruptedException;

  protected abstract void phaseFour();

  protected abstract void phaseFive();

  protected abstract void phaseSix() throws ExecutionException, InterruptedException;

  public AbstractIntegration(final BioSamplesClient webinClient) {
    this.webinClient = webinClient;
    this.noAuthClient =
        webinClient
            .getPublicClient()
            .orElseThrow(() -> new IntegrationTestFailException("Could not create public client"));
  }

  @Override
  public int getExitCode() {
    return exitCode;
  }

  @Override
  public void run(final ApplicationArguments args) throws Exception {
    final Phase phase = Phase.readPhaseFromArguments(args);
    switch (phase) {
      case ONE:
        phaseOne();
        TimeUnit.SECONDS.sleep(1);
        break;
      case TWO:
        phaseTwo();
        TimeUnit.SECONDS.sleep(1);
        break;
      case THREE:
        phaseThree();
        TimeUnit.SECONDS.sleep(1);
        break;
      case FOUR:
        phaseFour();
        TimeUnit.SECONDS.sleep(1);
        break;
      case FIVE:
        phaseFive();
        TimeUnit.SECONDS.sleep(1);
        break;
      case SIX:
        phaseSix();
        TimeUnit.SECONDS.sleep(1);
        break;
      default:
        log.warn("Invalid integration test phase {}", phase);
        break;
    }

    close();
    exitCode = 0;
  }

  private void close() {
    // do nothing
  }

  // For unit testing, we will consider name is unique, so we can fetch sample
  // uniquely from name
  Optional<Sample> fetchUniqueSampleByName(final String name) {
    final Optional<Sample> optionalSample;
    final Filter nameFilter = FilterBuilder.create().onName(name).build();
    final Iterator<EntityModel<Sample>> resourceIterator =
        noAuthClient.fetchSampleResourceAll(Collections.singletonList(nameFilter)).iterator();

    if (resourceIterator.hasNext()) {
      optionalSample = Optional.ofNullable(resourceIterator.next().getContent());
    } else {
      optionalSample = Optional.empty();
    }

    if (resourceIterator.hasNext()) {
      throw new IntegrationTestFailException("More than one sample present with the given name");
    }

    return optionalSample;
  }

  Sample fetchByNameOrElseThrow(final String name, final Phase phase) {
    return fetchUniqueSampleByName(name)
        .orElseThrow(
            () ->
                new IntegrationTestFailException(
                    "Sample does not exist, sample name: " + name, phase));
  }

  void fetchByNameAndThrowOrElsePersist(final Sample sample) {
    final Optional<Sample> optionalSample = fetchUniqueSampleByName(sample.getName());
    if (optionalSample.isPresent()) {
      throw new IntegrationTestFailException(
          "RestIntegration test sample should not be available during phase 1", Phase.ONE);
    } else {
      final EntityModel<Sample> resource = webinClient.persistSampleResource(sample);
      final Sample sampleContent = resource.getContent();
      final Attribute sraAccessionAttribute =
          sampleContent.getAttributes().stream()
              .filter(attribute -> attribute.getType().equals("SRA accession"))
              .findFirst()
              .get();

      sample.getAttributes().add(sraAccessionAttribute);

      final Sample testSampleWithAccession =
          Sample.Builder.fromSample(sample)
              .withAccession(Objects.requireNonNull(sampleContent).getAccession())
              .withStatus(sampleContent.getStatus())
              .build();

      if (!testSampleWithAccession.equals(sampleContent)) {
        throw new IntegrationTestFailException(
            "Expected response (" + sampleContent + ") to equal submission (" + sample + ")");
      }
    }
  }
}
