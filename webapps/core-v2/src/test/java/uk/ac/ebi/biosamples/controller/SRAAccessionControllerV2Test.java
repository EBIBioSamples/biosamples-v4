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
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.ac.ebi.biosamples.service.SampleService;

@ExtendWith(MockitoExtension.class)
class SRAAccessionControllerV2Test {
  @Mock private SampleService sampleService;

  @Test
  void generateOneSRAAccessionReturnsGeneratedAccession() {
    when(sampleService.generateOneSRAAccession()).thenReturn("ERS1");

    final SRAAccessionControllerV2 controller = new SRAAccessionControllerV2(sampleService);

    assertThat(controller.generateOneSRAAccession().getBody()).isEqualTo("ERS1");
  }

  @Test
  void generateOneSRAAccessionConvertsServiceFailureToInternalServerError() {
    when(sampleService.generateOneSRAAccession()).thenThrow(new RuntimeException("boom"));
    final SRAAccessionControllerV2 controller = new SRAAccessionControllerV2(sampleService);

    assertThatThrownBy(controller::generateOneSRAAccession)
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> {
              assertThat(exception.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
              assertThat(exception.getReason()).isEqualTo("boom");
            });
  }
}
