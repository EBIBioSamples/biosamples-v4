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
package uk.ac.ebi.biosamples.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class GlobalExceptions {
  @ResponseStatus(
      value = HttpStatus.BAD_REQUEST,
      reason = "New sample submission should not contain an accession")
  public static class SampleWithAccessionSubmissionException extends RuntimeException {}

  @ResponseStatus(
      value = HttpStatus.BAD_REQUEST,
      reason = "Sample accession must match URL accession") // 400
  public static class SampleAccessionMismatchException extends RuntimeException {}

  @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Sample accession does not exist") // 400
  public static class SampleAccessionDoesNotExistException extends RuntimeException {}

  public static class SampleNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1376682660925892995L;
  }

  public static class SampleValidationException extends RuntimeException {
    public SampleValidationException(String message) {
      super(message);
    }
  }

  public static class SchemaValidationException extends RuntimeException {
    public SchemaValidationException(String message, Exception e) {
      super(message, e);
    }

    public SchemaValidationException(String message) {
      super(message);
    }
  }

  @ResponseStatus(
      value = HttpStatus.FORBIDDEN,
      reason =
          "This sample is private and not available for browsing. If you think this is an error and/or you should have access please contact the BioSamples Helpdesk at biosamples@ebi.ac.uk")
  public static class SampleNotAccessibleException extends RuntimeException {}

  public static class AccessControlException extends RuntimeException {
    public AccessControlException(String message) {
      super(message);
    }

    public AccessControlException(String message, Throwable e) {
      super(message, e);
    }
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public static class SampleValidationControllerException extends RuntimeException {
    private static final long serialVersionUID = -7937033504537036300L;

    public SampleValidationControllerException(String message) {
      super(message);
    }
  }

  @ResponseStatus(
      value = HttpStatus.BAD_REQUEST,
      reason = "Curation Link must specify a domain") // 400
  public static class CurationLinkDomainMissingException extends RuntimeException {}

  @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Sample must specify a domain") // 400
  public static class DomainMissingException extends RuntimeException {}

  @ResponseStatus(
      value = HttpStatus.BAD_REQUEST,
      reason = "Structured data must have a domain") // 400
  public static class StructuredDataDomainMissingException extends RuntimeException {}

  @ResponseStatus(
      value = HttpStatus.FORBIDDEN,
      reason =
          "You don't have access to the sample structured data. If you think this is an error and/or you should have access please contact the BioSamples Helpdesk at biosamples@ebi.ac.uk") // 403
  public static class StructuredDataNotAccessibleException extends RuntimeException {}

  @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Sample domain mismatch") // 400
  public static class SampleDomainMismatchException extends RuntimeException {}

  @ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Unauthorized WEBIN user")
  public static class WebinUserLoginUnauthorizedException extends RuntimeException {}

  @ResponseStatus(
      value = HttpStatus.BAD_REQUEST,
      reason = "Structured data must have a webin submission account id") // 400
  public static class StructuredDataWebinIdMissingException extends RuntimeException {}

  @ResponseStatus(
      value = HttpStatus.BAD_REQUEST,
      reason = "You must provide a bearer token to be able to submit") // 400
  public static class WebinTokenInvalidException extends RuntimeException {}

  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public static class UploadInvalidException extends RuntimeException {
    public UploadInvalidException(final String collect) {
      super(collect);
    }
  }

  @ResponseStatus(
      value = HttpStatus.BAD_REQUEST,
      reason =
          "Validation of taxonomy failed against the ENA taxonomy service. The Organism attribute is either invalid or not submittable")
  public static class ENATaxonUnresolvedException extends RuntimeException {}

  @ResponseStatus(
      value = HttpStatus.BAD_REQUEST,
      reason = "Sample must match URL or be omitted") // 400
  public static class SampleNotMatchException extends RuntimeException {}

  @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Bulk accessioning failure")
  public static class BulkAccessionFailureExceptionV2 extends RuntimeException {
    public BulkAccessionFailureExceptionV2(String message) {
      super(message);
    }
  }
}
