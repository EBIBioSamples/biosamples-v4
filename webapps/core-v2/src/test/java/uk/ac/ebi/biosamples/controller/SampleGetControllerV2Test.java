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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.WebinAuthenticationService;

@ExtendWith(MockitoExtension.class)
class SampleGetControllerV2Test {
  private static final String WEBIN_ID = "Webin-12345";

  @Mock private SampleService sampleService;
  @Mock private WebinAuthenticationService webinAuthenticationService;
  @Mock private Authentication authentication;

  private SampleGetControllerV2 controller;

  @BeforeEach
  void setUp() {
    controller = new SampleGetControllerV2(sampleService, webinAuthenticationService);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(sampleService.getPrinciple(authentication)).thenReturn(WEBIN_ID);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getSampleV2ReturnsAccessibleSample() {
    final Sample sample =
        new Sample.Builder("sample-1", "SAMEA1")
            .withRelease(Instant.now().minusSeconds(3600))
            .build();
    when(sampleService.fetch("SAMEA1", false)).thenReturn(Optional.of(sample));

    assertThat(controller.getSampleV2("SAMEA1")).isSameAs(sample);
    verify(webinAuthenticationService).isSampleAccessible(sample, WEBIN_ID);
  }

  @Test
  void getSampleV2ThrowsWhenSampleDoesNotExist() {
    when(sampleService.fetch("SAMEA1", false)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.getSampleV2("SAMEA1"))
        .isInstanceOf(GlobalExceptions.SampleNotFoundException.class);
  }

  @Test
  void getSampleV2PropagatesInaccessibleSample() {
    final Sample sample =
        new Sample.Builder("sample-1", "SAMEA1")
            .withRelease(Instant.now().plusSeconds(3600))
            .build();
    when(sampleService.fetch("SAMEA1", false)).thenReturn(Optional.of(sample));
    doThrow(new GlobalExceptions.SampleNotAccessibleException())
        .when(webinAuthenticationService)
        .isSampleAccessible(any(Sample.class), any());

    assertThatThrownBy(() -> controller.getSampleV2("SAMEA1"))
        .isInstanceOf(GlobalExceptions.SampleNotAccessibleException.class);
  }
}
