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
package uk.ac.ebi.biosamples.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.SampleStatus;
import uk.ac.ebi.biosamples.core.service.SampleValidator;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repository.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleReadService;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
import uk.ac.ebi.biosamples.security.service.BioSamplesCrossSourceIngestAccessControlService;

@ExtendWith(MockitoExtension.class)
class SampleServiceTest {
  private static final String WEBIN_ID = "Webin-12345";

  @Mock private MongoAccessionService mongoAccessionService;
  @Mock private MongoSampleRepository mongoSampleRepository;
  @Mock private MongoSampleToSampleConverter mongoSampleToSampleConverter;
  @Mock private SampleToMongoSampleConverter sampleToMongoSampleConverter;
  @Mock private SampleValidator sampleValidator;
  @Mock private SampleReadService sampleReadService;
  @Mock private MessagingService messagingService;
  @Mock private BioSamplesProperties bioSamplesProperties;
  @Mock private BioSamplesCrossSourceIngestAccessControlService accessControlService;
  @Mock private MongoSample mongoSample;

  @Captor private ArgumentCaptor<Sample> sampleCaptor;

  @InjectMocks private SampleService sampleService;

  @BeforeEach
  void setUp() {
    lenient().when(sampleValidator.validate(any(Sample.class))).thenReturn(Collections.emptyList());
  }

  @Test
  void persistSampleV2PromotesNewPastReleasePrivateSampleToPublicBeforeAccessioning() {
    final Sample sample =
        new Sample.Builder("sample-1", SampleStatus.PRIVATE)
            .withRelease(Instant.now().minusSeconds(3600))
            .withWebinSubmissionAccountId(WEBIN_ID)
            .build();
    when(mongoAccessionService.generateAccession(any(Sample.class), eq(true)))
        .thenAnswer(
            invocation ->
                Sample.Builder.fromSample(invocation.getArgument(0))
                    .withAccession("SAMEA1")
                    .build());

    final Sample persisted = sampleService.persistSampleV2(sample, null, false);

    verify(mongoAccessionService).generateAccession(sampleCaptor.capture(), eq(true));
    assertThat(sampleCaptor.getValue().getStatus()).isEqualTo(SampleStatus.PUBLIC);
    assertThat(persisted.getStatus()).isEqualTo(SampleStatus.PUBLIC);
  }

  @Test
  void persistSampleV2KeepsNewFutureReleaseSamplePrivateEvenWhenRequestAsksForPublic() {
    final Sample sample =
        new Sample.Builder("sample-1", SampleStatus.PUBLIC)
            .withRelease(Instant.now().plusSeconds(3600))
            .withWebinSubmissionAccountId(WEBIN_ID)
            .build();
    when(mongoAccessionService.generateAccession(any(Sample.class), eq(true)))
        .thenAnswer(
            invocation ->
                Sample.Builder.fromSample(invocation.getArgument(0))
                    .withAccession("SAMEA1")
                    .build());

    final Sample persisted = sampleService.persistSampleV2(sample, null, false);

    verify(mongoAccessionService).generateAccession(sampleCaptor.capture(), eq(true));
    assertThat(sampleCaptor.getValue().getStatus()).isEqualTo(SampleStatus.PRIVATE);
    assertThat(persisted.getStatus()).isEqualTo(SampleStatus.PRIVATE);
  }

  @Test
  void accessionSamplePromotesPastReleasePrivateSampleToPublicBeforeAccessioning() {
    final Sample sample =
        new Sample.Builder("sample-1", SampleStatus.PRIVATE)
            .withRelease(Instant.now().minusSeconds(3600))
            .withWebinSubmissionAccountId(WEBIN_ID)
            .build();
    when(mongoAccessionService.generateAccession(any(Sample.class), eq(true)))
        .thenAnswer(
            invocation ->
                Sample.Builder.fromSample(invocation.getArgument(0))
                    .withAccession("SAMEA1")
                    .build());

    final Sample accessioned = sampleService.accessionSample(sample);

    verify(mongoAccessionService).generateAccession(sampleCaptor.capture(), eq(true));
    assertThat(sampleCaptor.getValue().getStatus()).isEqualTo(SampleStatus.PUBLIC);
    assertThat(accessioned.getStatus()).isEqualTo(SampleStatus.PUBLIC);
  }

  @Test
  void accessionSampleKeepsFutureReleaseSamplePrivateEvenWhenRequestAsksForPublic() {
    final Sample sample =
        new Sample.Builder("sample-1", SampleStatus.PUBLIC)
            .withRelease(Instant.now().plusSeconds(3600))
            .withWebinSubmissionAccountId(WEBIN_ID)
            .build();
    when(mongoAccessionService.generateAccession(any(Sample.class), eq(true)))
        .thenAnswer(
            invocation ->
                Sample.Builder.fromSample(invocation.getArgument(0))
                    .withAccession("SAMEA1")
                    .build());

    final Sample accessioned = sampleService.accessionSample(sample);

    verify(mongoAccessionService).generateAccession(sampleCaptor.capture(), eq(true));
    assertThat(sampleCaptor.getValue().getStatus()).isEqualTo(SampleStatus.PRIVATE);
    assertThat(accessioned.getStatus()).isEqualTo(SampleStatus.PRIVATE);
  }

  @Test
  void persistSampleV2DoesNotPromoteSuppressedUpdateWithPastReleaseToPublic() {
    final Sample oldSample =
        new Sample.Builder("sample-1", "SAMEA1", SampleStatus.SUPPRESSED)
            .withRelease(Instant.now().minusSeconds(7200))
            .withWebinSubmissionAccountId(WEBIN_ID)
            .withTaxId(9606L)
            .build();
    final Sample update =
        new Sample.Builder("sample-1", "SAMEA1", SampleStatus.SUPPRESSED)
            .withRelease(Instant.now().minusSeconds(3600))
            .withWebinSubmissionAccountId(WEBIN_ID)
            .build();
    final AtomicReference<Sample> convertedSample = new AtomicReference<>();
    when(mongoAccessionService.generateOneSRAAccession()).thenReturn("ERS1");
    when(sampleToMongoSampleConverter.convert(any(Sample.class)))
        .thenAnswer(
            invocation -> {
              convertedSample.set(invocation.getArgument(0));
              return mongoSample;
            });
    when(mongoSampleRepository.save(mongoSample)).thenReturn(mongoSample);
    when(mongoSampleToSampleConverter.apply(mongoSample))
        .thenAnswer(invocation -> convertedSample.get());

    final Sample persisted = sampleService.persistSampleV2(update, oldSample, true);

    verify(sampleToMongoSampleConverter).convert(sampleCaptor.capture());
    assertThat(sampleCaptor.getValue().getStatus()).isEqualTo(SampleStatus.SUPPRESSED);
    assertThat(persisted.getStatus()).isEqualTo(SampleStatus.SUPPRESSED);
  }

  @Test
  void getPrincipleReturnsNullWhenAuthenticationIsMissing() {
    assertThat(sampleService.getPrinciple(null)).isNull();
  }

  @Test
  void getPrincipleReturnsPlainPrincipalString() {
    final TestingAuthenticationToken authentication =
        new TestingAuthenticationToken(WEBIN_ID, "password");

    assertThat(sampleService.getPrinciple(authentication)).isEqualTo(WEBIN_ID);
  }

  @Test
  void fetchDelegatesToSampleReadService() {
    final Sample sample = new Sample.Builder("sample-1", "SAMEA1").build();
    when(sampleReadService.fetch("SAMEA1", false)).thenReturn(Optional.of(sample));

    assertThat(sampleService.fetch("SAMEA1", false)).contains(sample);
  }
}
