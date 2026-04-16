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
import java.util.Iterator;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Attribute;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.SampleStatus;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.core.service.FilterBuilder;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
@Order(2)
public class RestSampleStatusTransitionIntegration extends AbstractIntegration {
  private static final String SAMPLE_NAME =
      "RestSampleStatusTransitionIntegration_sample_suppressed";
  private static final String PRIVATE_SAMPLE_NAME =
      "RestSampleStatusTransitionIntegration_sample_private";

  public RestSampleStatusTransitionIntegration(final BioSamplesClient client) {
    super(client);
  }

  @Override
  protected void phaseOne() {
    if (fetchUniqueSampleByNameForWebin(SAMPLE_NAME).isPresent()) {
      throw new IntegrationTestFailException(
          "Transition test sample should not be present during phase 1", Phase.ONE);
    }
    if (fetchUniqueSampleByNameForWebin(PRIVATE_SAMPLE_NAME).isPresent()) {
      throw new IntegrationTestFailException(
          "Private transition test sample should not be present during phase 1", Phase.ONE);
    }

    final EntityModel<Sample> persisted = webinClient.persistSampleResource(getSuppressedSample());
    if (persisted.getContent() == null) {
      throw new IntegrationTestFailException(
          "Failed to persist transition test sample in phase 1", Phase.ONE);
    }
    final EntityModel<Sample> privatePersisted =
        webinClient.persistSampleResource(getPrivateSampleWithPastReleaseDate());
    if (privatePersisted.getContent() == null) {
      throw new IntegrationTestFailException(
          "Failed to persist private transition test sample in phase 1", Phase.ONE);
    }
  }

  @Override
  protected void phaseTwo() {
    final Sample existing =
        fetchUniqueSampleByNameForWebin(SAMPLE_NAME)
            .orElseThrow(
                () ->
                    new IntegrationTestFailException(
                        "Failed to fetch transition test sample in phase 2", Phase.TWO));

    final Sample updateToPublic =
        Sample.Builder.fromSample(existing)
            .withStatus(SampleStatus.PUBLIC)
            .withRelease(Instant.parse("2016-04-01T11:36:57.00Z"))
            .build();

    boolean transitionBlocked = false;
    try {
      webinClient.persistSampleResource(updateToPublic);
    } catch (final Exception ignored) {
      transitionBlocked = true;
    }

    if (!transitionBlocked) {
      throw new IntegrationTestFailException(
          "SUPPRESSED -> PUBLIC transition should be blocked", Phase.TWO);
    }

    final Sample fetchedAfterAttempt =
        webinClient
            .fetchSampleResource(existing.getAccession())
            .orElseThrow(
                () ->
                    new IntegrationTestFailException(
                        "Sample should still exist after blocked transition", Phase.TWO))
            .getContent();

    if (fetchedAfterAttempt == null || fetchedAfterAttempt.getStatus() != SampleStatus.SUPPRESSED) {
      throw new IntegrationTestFailException(
          "Blocked transition should keep sample in SUPPRESSED state", Phase.TWO);
    }

    final Sample privateSample =
        fetchUniqueSampleByNameForWebin(PRIVATE_SAMPLE_NAME)
            .orElseThrow(
                () ->
                    new IntegrationTestFailException(
                        "Failed to fetch private transition test sample in phase 2", Phase.TWO));

    final Sample updatePrivateToPublic =
        Sample.Builder.fromSample(privateSample)
            .withStatus(SampleStatus.PUBLIC)
            .withRelease(Instant.now())
            .build();

    webinClient.persistSampleResource(updatePrivateToPublic);

    final Sample privateSampleAfterTransition =
        webinClient
            .fetchSampleResource(privateSample.getAccession())
            .orElseThrow(
                () ->
                    new IntegrationTestFailException(
                        "Private transition sample should still exist after update", Phase.TWO))
            .getContent();

    if (privateSampleAfterTransition == null
        || privateSampleAfterTransition.getStatus() != SampleStatus.PUBLIC) {
      throw new IntegrationTestFailException(
          "PRIVATE -> PUBLIC transition should be allowed when release <= now", Phase.TWO);
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

  private Optional<Sample> fetchUniqueSampleByNameForWebin(final String name) {
    final Filter nameFilter = FilterBuilder.create().onName(name).build();
    final Iterator<EntityModel<Sample>> iterator =
        webinClient.fetchSampleResourceAll(Collections.singletonList(nameFilter)).iterator();

    final Optional<Sample> optionalSample;
    if (iterator.hasNext()) {
      optionalSample = Optional.ofNullable(iterator.next().getContent());
    } else {
      optionalSample = Optional.empty();
    }

    if (iterator.hasNext()) {
      throw new IntegrationTestFailException(
          "More than one sample present with the given name for transition test");
    }

    return optionalSample;
  }

  private Sample getSuppressedSample() {
    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    attributes.add(Attribute.build("INSDC status", "suppressed"));

    return new Sample.Builder(SAMPLE_NAME)
        .withTaxId(9606L)
        .withStatus(SampleStatus.SUPPRESSED)
        .withUpdate(Instant.parse("2016-05-05T11:36:57.00Z"))
        .withRelease(Instant.parse("2016-04-01T11:36:57.00Z"))
        .withWebinSubmissionAccountId(defaultWebinIdForIntegrationTests)
        .withAttributes(attributes)
        .build();
  }

  private Sample getPrivateSampleWithPastReleaseDate() {
    final SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));

    return new Sample.Builder(PRIVATE_SAMPLE_NAME)
        .withTaxId(9606L)
        .withStatus(SampleStatus.PRIVATE)
        .withUpdate(Instant.parse("2016-05-05T11:36:57.00Z"))
        .withRelease(Instant.parse("2016-04-01T11:36:57.00Z"))
        .withWebinSubmissionAccountId(defaultWebinIdForIntegrationTests)
        .withAttributes(attributes)
        .build();
  }
}
