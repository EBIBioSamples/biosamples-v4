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
package uk.ac.ebi.biosamples.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.SubmissionReceipt;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.WebinAuthenticationService;
import uk.ac.ebi.biosamples.service.validation.SchemaValidationService;

@ExtendWith(MockitoExtension.class)
class BulkActionControllerV2UnitTest {
  private static final String WEBIN_ID = "Webin-12345";
  private static final String SUPER_USER = "Webin-40894";

  @Mock private SampleService sampleService;
  @Mock private WebinAuthenticationService webinAuthenticationService;
  @Mock private SchemaValidationService schemaValidationService;
  @Mock private BioSamplesProperties bioSamplesProperties;
  @Mock private Authentication authentication;

  private BulkActionControllerV2 controller;

  @BeforeEach
  void setUp() {
    controller =
        new BulkActionControllerV2(
            sampleService,
            webinAuthenticationService,
            schemaValidationService,
            bioSamplesProperties,
            new ObjectMapper());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getV2ReturnsOnlyFoundAndAccessibleSamples() {
    authenticateAs(WEBIN_ID);
    final Sample accessible = sample("sample-1", "SAMEA1");
    final Sample inaccessible = sample("sample-2", "SAMEA2");
    when(sampleService.fetch("SAMEA1", false)).thenReturn(Optional.of(accessible));
    when(sampleService.fetch("SAMEA2", false)).thenReturn(Optional.of(inaccessible));
    when(sampleService.fetch("missing", false)).thenReturn(Optional.empty());
    doAnswer(
            invocation -> {
              final Sample sample = invocation.getArgument(0);
              if ("SAMEA2".equals(sample.getAccession())) {
                throw new GlobalExceptions.SampleNotAccessibleException();
              }
              return null;
            })
        .when(webinAuthenticationService)
        .isSampleAccessible(any(Sample.class), eq(WEBIN_ID));

    final ResponseEntity<Map<String, Sample>> response =
        controller.getV2(List.of(" SAMEA1 ", "SAMEA2", "missing"), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsOnlyKeys("SAMEA1");
    assertThat(response.getBody().get("SAMEA1")).isSameAs(accessible);
  }

  @Test
  void getV2RejectsNullAccessions() {
    authenticateAs(WEBIN_ID);

    assertThatThrownBy(() -> controller.getV2(null, null))
        .isInstanceOf(GlobalExceptions.BulkFetchInvalidRequestException.class);
  }

  @Test
  void accessionV2RejectsSampleWithAccession() {
    authenticateAs(WEBIN_ID);
    final Sample sample = sample("sample-1", "SAMEA1");

    assertThatThrownBy(() -> controller.accessionV2(List.of(sample)))
        .isInstanceOf(GlobalExceptions.SampleWithAccessionSubmissionException.class);
  }

  @Test
  void postV2PersistsSamplesAndReturnsCreatedReceipt() {
    authenticateAs(WEBIN_ID);
    final Sample request = sample("sample-1");
    final Sample withWebin =
        Sample.Builder.fromSample(request).withWebinSubmissionAccountId(WEBIN_ID).build();
    final Sample persisted = sample("sample-1", "SAMEA1");
    stubValidatedPersistFlow(request, withWebin, persisted, WEBIN_ID, false);

    final ResponseEntity<SubmissionReceipt> response = controller.postV2(List.of(request));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getSamples()).containsExactly(persisted);
    assertThat(response.getBody().getErrors()).isEmpty();
    verify(schemaValidationService).validate(any(Sample.class), eq(WEBIN_ID));
  }

  @Test
  void postV2ReturnsValidationErrorsWithoutPersistingInvalidSample() {
    authenticateAs(WEBIN_ID);
    final Sample request = sample("sample-1");
    final Sample withWebin =
        Sample.Builder.fromSample(request).withWebinSubmissionAccountId(WEBIN_ID).build();
    stubSubmissionSetup(request, withWebin, WEBIN_ID, false);
    doThrow(new GlobalExceptions.SchemaValidationException("plain validation error"))
        .when(schemaValidationService)
        .validate(any(Sample.class), eq(WEBIN_ID));

    final ResponseEntity<SubmissionReceipt> response = controller.postV2(List.of(request));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getSamples()).isEmpty();
    assertThat(response.getBody().getErrors()).hasSize(1);
    assertThat(response.getBody().getErrors().get(0).getSampleName()).isEqualTo("sample-1");
    assertThat(response.getBody().getErrors().get(0).getErrors().get(0).getErrors())
        .containsExactly("plain validation error");
    verify(sampleService, never()).persistSampleV2(any(), any(), eq(false));
  }

  @Test
  void postV2NoValidationRejectsNonSuperUser() {
    authenticateAs(WEBIN_ID);
    when(webinAuthenticationService.isWebinSuperUser(WEBIN_ID)).thenReturn(false);

    assertThatThrownBy(() -> controller.postV2NoValidation(List.of(sample("sample-1"))))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_ACCEPTABLE));
  }

  @Test
  void postV2NoValidationPersistsForSuperUserWithoutSchemaValidation() {
    authenticateAs(SUPER_USER);
    final Sample request = sample("sample-1");
    final Sample withWebin =
        Sample.Builder.fromSample(request).withWebinSubmissionAccountId(SUPER_USER).build();
    final Sample persisted = sample("sample-1", "SAMEA1");
    when(webinAuthenticationService.isWebinSuperUser(SUPER_USER)).thenReturn(true);
    stubValidatedPersistFlow(request, withWebin, persisted, SUPER_USER, true);

    final ResponseEntity<List<Sample>> response = controller.postV2NoValidation(List.of(request));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).containsExactly(persisted);
    verify(schemaValidationService, never()).validate(any(Sample.class), eq(SUPER_USER));
  }

  @Test
  void validateV2ReturnsValidationErrors() {
    authenticateAs(WEBIN_ID);
    final Sample request = sample("sample-1");
    doThrow(new GlobalExceptions.SchemaValidationException("plain validation error"))
        .when(schemaValidationService)
        .validate(request, WEBIN_ID);

    final ResponseEntity<SubmissionReceipt> response = controller.validateV2(List.of(request));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getSamples()).isNull();
    assertThat(response.getBody().getErrors()).hasSize(1);
    assertThat(response.getBody().getErrors().get(0).getErrors().get(0).getErrors())
        .containsExactly("plain validation error");
  }

  @Test
  void validateV2ReturnsNoErrorsWhenSchemaValidationPasses() {
    authenticateAs(WEBIN_ID);

    final ResponseEntity<SubmissionReceipt> response =
        controller.validateV2(List.of(sample("sample-1")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getSamples()).isNull();
    assertThat(response.getBody().getErrors()).isEmpty();
  }

  private void authenticateAs(final String principle) {
    when(sampleService.getPrinciple(authentication)).thenReturn(principle);
  }

  private void stubValidatedPersistFlow(
      final Sample request,
      final Sample withWebin,
      final Sample persisted,
      final String principle,
      final boolean isSuperUser) {
    stubSubmissionSetup(request, withWebin, principle, isSuperUser);
    when(sampleService.persistSampleV2(any(Sample.class), isNull(), eq(isSuperUser)))
        .thenReturn(persisted);
  }

  private void stubSubmissionSetup(
      final Sample request,
      final Sample withWebin,
      final String principle,
      final boolean isSuperUser) {
    final Instant now = Instant.now();
    when(webinAuthenticationService.isWebinSuperUser(principle)).thenReturn(isSuperUser);
    when(sampleService.validateSampleWithAccessionsAgainstConditionsAndGetOldSample(
            request, isSuperUser))
        .thenReturn(Optional.empty());
    when(sampleService.handleSampleRelationshipsV2(request, Optional.empty(), isSuperUser))
        .thenReturn(null);
    when(webinAuthenticationService.handleWebinUserSubmission(request, principle, Optional.empty()))
        .thenReturn(withWebin);
    when(sampleService.defineCreateDate(any(Sample.class), eq(isSuperUser))).thenReturn(now);
    when(sampleService.defineSubmittedDate(any(Sample.class), eq(isSuperUser))).thenReturn(now);
  }

  private Sample sample(final String name) {
    return new Sample.Builder(name).withRelease(Instant.now().minusSeconds(3600)).build();
  }

  private Sample sample(final String name, final String accession) {
    return new Sample.Builder(name, accession)
        .withRelease(Instant.now().minusSeconds(3600))
        .build();
  }
}
