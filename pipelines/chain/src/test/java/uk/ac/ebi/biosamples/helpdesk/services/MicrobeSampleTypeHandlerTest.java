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
package uk.ac.ebi.biosamples.helpdesk.services;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.springframework.hateoas.EntityModel;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Sample;

public class MicrobeSampleTypeHandlerTest {

  @Test
  public void fetchSamples_collects_all_non_null_samples_into_array_list() {
    final BioSamplesClient webinClient = mock(BioSamplesClient.class);
    final BioSamplesClient publicClient = mock(BioSamplesClient.class);
    when(webinClient.getPublicClient()).thenReturn(Optional.of(publicClient));
    final MicrobeSampleTypeHandler service = new MicrobeSampleTypeHandler(webinClient);
    final EntityModel<Sample> nullContentModel = mock(EntityModel.class);
    when(nullContentModel.getContent()).thenReturn(null);

    final Sample releasedSample =
        Sample.build(
            "released",
            "SAMEA1",
            null,
            null,
            null,
            null,
            null,
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now(),
            Instant.now(),
            null,
            null,
            null,
            null,
            null);
    final Sample secondSample =
        Sample.build(
            "second",
            "SAMEA2",
            null,
            null,
            null,
            null,
            null,
            Instant.now().plus(1, ChronoUnit.DAYS),
            Instant.now(),
            Instant.now(),
            null,
            null,
            null,
            null,
            null);

    when(publicClient.fetchSampleResourceAllWithSize(eq(""), anyCollection(), anyInt()))
        .thenReturn(
            List.of(
                EntityModel.of(releasedSample), EntityModel.of(secondSample), nullContentModel));

    final List<Sample> samples = service.fetchSamples(20);

    assertTrue(samples instanceof ArrayList);
    assertThat(samples.stream().map(Sample::getAccession).toList(), contains("SAMEA1", "SAMEA2"));
  }
}
