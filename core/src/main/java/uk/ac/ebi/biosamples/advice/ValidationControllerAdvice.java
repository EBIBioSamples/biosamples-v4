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
package uk.ac.ebi.biosamples.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.ac.ebi.biosamples.core.model.ValidationReport;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;

@ControllerAdvice
public class ValidationControllerAdvice {

  @ExceptionHandler(GlobalExceptions.SampleMandatoryFieldsMissingException.class)
  public ResponseEntity<ValidationReport> handleValidationException(
      GlobalExceptions.SampleMandatoryFieldsMissingException e) {
    ValidationReport report = e.getValidationReport();
    if (report == null) {
        // Fallback for old cases
        report = new ValidationReport();
        report.addInvalidType(e.getMessage());
    }
    return new ResponseEntity<>(report, HttpStatus.BAD_REQUEST);
  }
}
