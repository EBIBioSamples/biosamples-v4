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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.SubmittedViaType;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;
import uk.ac.ebi.biosamples.security.service.BioSamplesCrossSourceIngestAccessControlService;

@ExtendWith(MockitoExtension.class)
class WebinAuthenticationServiceTest {
  private static final String SUPER_USER = "Webin-40894";
  private static final String SUBMITTER = "Webin-12345";
  private static final String OTHER_USER = "Webin-99999";

  @Mock private SampleService sampleService;
  @Mock private BioSamplesCrossSourceIngestAccessControlService accessControlService;
  @Mock private BioSamplesProperties bioSamplesProperties;

  private WebinAuthenticationService webinAuthenticationService;

  @BeforeEach
  void setUp() {
    lenient().when(bioSamplesProperties.getBiosamplesClientWebinUsername()).thenReturn(SUPER_USER);
    webinAuthenticationService =
        new WebinAuthenticationService(sampleService, accessControlService, bioSamplesProperties);
  }

  @Test
  void buildSampleWithWebinIdSetsWebinIdAndClearsDomain() {
    final Sample sample =
        new Sample.Builder("sample-1").withDomain("self.Example").withRelease(past()).build();

    final Sample result = webinAuthenticationService.buildSampleWithWebinId(sample, SUBMITTER);

    assertThat(result.getWebinSubmissionAccountId()).isEqualTo(SUBMITTER);
    assertThat(result.getDomain()).isNull();
  }

  @Test
  void handleNewSubmissionUsesAuthenticatedWebinForNormalUser() {
    final Sample sample =
        new Sample.Builder("sample-1")
            .withRelease(past())
            .withWebinSubmissionAccountId(OTHER_USER)
            .build();

    final Sample result =
        webinAuthenticationService.handleWebinUserSubmission(sample, SUBMITTER, Optional.empty());

    assertThat(result.getWebinSubmissionAccountId()).isEqualTo(SUBMITTER);
  }

  @Test
  void handleNewSubmissionLetsSuperUserSubmitForProvidedWebinId() {
    final Sample sample =
        new Sample.Builder("sample-1")
            .withRelease(past())
            .withWebinSubmissionAccountId(SUBMITTER)
            .build();

    final Sample result =
        webinAuthenticationService.handleWebinUserSubmission(sample, SUPER_USER, Optional.empty());

    assertThat(result.getWebinSubmissionAccountId()).isEqualTo(SUBMITTER);
  }

  @Test
  void handleNormalUpdateRejectsNonSubmitter() {
    final Sample oldSample =
        new Sample.Builder("sample-1", "SAMEA1")
            .withRelease(past())
            .withWebinSubmissionAccountId(SUBMITTER)
            .build();
    final Sample update =
        new Sample.Builder("sample-1", "SAMEA1")
            .withRelease(past())
            .withWebinSubmissionAccountId(OTHER_USER)
            .build();

    assertThatThrownBy(
            () ->
                webinAuthenticationService.handleWebinUserSubmission(
                    update, OTHER_USER, Optional.of(oldSample)))
        .isInstanceOf(GlobalExceptions.NonSubmitterUpdateAttemptException.class);
  }

  @Test
  void handleNormalUpdateKeepsSubmitterAndRunsSourceAccessChecks() {
    final Sample oldSample =
        new Sample.Builder("sample-1", "SAMEA1")
            .withRelease(past())
            .withWebinSubmissionAccountId(SUBMITTER)
            .build();
    final Sample update =
        new Sample.Builder("sample-1", "SAMEA1")
            .withRelease(past())
            .withSubmittedVia(SubmittedViaType.JSON_API)
            .build();

    final Sample result =
        webinAuthenticationService.handleWebinUserSubmission(
            update, SUBMITTER, Optional.of(oldSample));

    assertThat(result.getWebinSubmissionAccountId()).isEqualTo(SUBMITTER);
    verify(accessControlService).accessControlPipelineImportedSamples(oldSample, update);
    verify(accessControlService)
        .accessControlWebinSourcedSampleByCheckingEnaChecklistAttribute(oldSample, update);
    verify(accessControlService)
        .accessControlWebinSourcedSampleByCheckingSubmittedViaType(oldSample, update);
  }

  @Test
  void isSampleAccessibleAllowsReleasedSampleWithoutUser() {
    final Sample sample = new Sample.Builder("sample-1", "SAMEA1").withRelease(past()).build();

    assertThatCode(() -> webinAuthenticationService.isSampleAccessible(sample, null))
        .doesNotThrowAnyException();
  }

  @Test
  void isSampleAccessibleRejectsPrivateSampleForDifferentUser() {
    final Sample sample =
        new Sample.Builder("sample-1", "SAMEA1")
            .withRelease(future())
            .withWebinSubmissionAccountId(SUBMITTER)
            .build();

    assertThatThrownBy(() -> webinAuthenticationService.isSampleAccessible(sample, OTHER_USER))
        .isInstanceOf(GlobalExceptions.SampleNotAccessibleException.class);
  }

  @Test
  void isSampleAccessibleAllowsPrivateSampleForSuperUser() {
    final Sample sample =
        new Sample.Builder("sample-1", "SAMEA1")
            .withRelease(future())
            .withWebinSubmissionAccountId(SUBMITTER)
            .build();

    assertThatCode(() -> webinAuthenticationService.isSampleAccessible(sample, SUPER_USER))
        .doesNotThrowAnyException();
  }

  private Instant past() {
    return Instant.now().minusSeconds(3600);
  }

  private Instant future() {
    return Instant.now().plusSeconds(3600);
  }
}
