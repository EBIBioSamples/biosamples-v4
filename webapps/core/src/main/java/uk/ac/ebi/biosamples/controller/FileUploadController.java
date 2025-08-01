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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.validation.Valid;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;
import uk.ac.ebi.biosamples.mongo.model.MongoFileUpload;
import uk.ac.ebi.biosamples.security.model.AuthToken;
import uk.ac.ebi.biosamples.security.service.AccessControlService;
import uk.ac.ebi.biosamples.service.upload.FileQueueService;
import uk.ac.ebi.biosamples.service.upload.FileUploadService;
import uk.ac.ebi.biosamples.utils.upload.FileUploadUtils;

@Controller
@RequestMapping("/upload")
public class FileUploadController {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final FileUploadService fileUploadService;
  private final FileQueueService fileQueueService;
  private final BioSamplesProperties bioSamplesProperties;
  private final AccessControlService accessControlService;

  public FileUploadController(
      final FileUploadService fileUploadService,
      final FileQueueService fileQueueService,
      final BioSamplesProperties bioSamplesProperties,
      final AccessControlService accessControlService) {
    this.fileUploadService = fileUploadService;
    this.fileQueueService = fileQueueService;
    this.bioSamplesProperties = bioSamplesProperties;
    this.accessControlService = accessControlService;
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping
  public ResponseEntity<byte[]> upload(
      @RequestParam("file") final MultipartFile file,
      @Valid final String hiddenChecklist,
      @Valid final String webinId)
      throws IOException {
    final FileUploadUtils fileUploadUtils = new FileUploadUtils();

    if (!isFileSizeExceeded(file)) {
      log.info("File size doesn't exceed limits");

      try {
        final File downloadableFile =
            fileUploadService.upload(file, hiddenChecklist, webinId, fileUploadUtils);
        final byte[] bytes = FileUtils.readFileToByteArray(downloadableFile);
        final HttpHeaders headers = setResponseHeadersSuccess(downloadableFile);

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
      } catch (final GlobalExceptions.UploadInvalidException e) {
        log.info("File upload failure " + e.getMessage());

        final Path temp = Files.createTempFile("failure_result_bad_req", ".txt");

        try (final BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
          writer.write(e.getMessage());
        }

        final File failedUploadMessageFile = temp.toFile();
        final byte[] bytes = FileUtils.readFileToByteArray(failedUploadMessageFile);
        final HttpHeaders headers = setResponseHeadersFailure(failedUploadMessageFile);

        return new ResponseEntity<>(bytes, headers, HttpStatus.BAD_REQUEST);
      } catch (final Exception e) {
        log.info("File upload failure - server error " + e.getMessage());

        final Path temp = Files.createTempFile("failure_result_server_error", ".txt");

        try (final BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
          writer.write(e.getMessage());
        }

        final File failedUploadMessageFile = temp.toFile();
        final byte[] bytes = FileUtils.readFileToByteArray(failedUploadMessageFile);
        final HttpHeaders headers = setResponseHeadersFailure(failedUploadMessageFile);

        return new ResponseEntity<>(bytes, headers, HttpStatus.INTERNAL_SERVER_ERROR);
      }
    } else {
      log.info("File size exceeds limits - queueing file for async submission");
      final Path temp = Files.createTempFile("queue_result", ".txt");

      try {
        final String fileId =
            fileQueueService.queueFileinMongoAndSendMessageToRabbitMq(
                file, hiddenChecklist, webinId);
        final File queuedUploadMessageFile = fileUploadUtils.writeQueueMessageToFile(fileId);
        final byte[] bytes = FileUtils.readFileToByteArray(queuedUploadMessageFile);
        final HttpHeaders headers = setResponseHeadersFailure(queuedUploadMessageFile);

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
      } catch (final Exception e) {
        try (final BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
          writer.write("Failure processing your submission");
        }

        final File queuedUploadMessageFile = temp.toFile();
        final byte[] bytes = FileUtils.readFileToByteArray(queuedUploadMessageFile);
        final HttpHeaders headers = setResponseHeadersFailure(queuedUploadMessageFile);

        return new ResponseEntity<>(bytes, headers, HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
  }

  private HttpHeaders setResponseHeadersSuccess(final File file) {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(new MediaType("text", "csv"));
    httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());

    return httpHeaders;
  }

  private HttpHeaders setResponseHeadersFailure(final File file) {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(new MediaType("text", "plain"));
    httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());

    return httpHeaders;
  }

  @GetMapping(value = "/downloadExampleFile")
  public ResponseEntity<byte[]> downloadExampleFile() throws IOException {
    final Path tempFile = Files.createTempFile("upload_example", ".tsv");
    final File pathFile = tempFile.toFile();

    FileUtils.copyInputStreamToFile(
        getClass().getClassLoader().getResourceAsStream("isa-example.tsv"), pathFile);
    final byte[] bytes = FileUtils.readFileToByteArray(pathFile);
    final HttpHeaders headers = setResponseHeadersSuccess(pathFile);

    return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
  }

  @GetMapping(
      value = "/poll",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> poll(
      @RequestParam(value = "searchSubmissionId") final String searchSubmissionId)
      throws JsonProcessingException {
    final MongoFileUpload mongoFileUpload = fileUploadService.getSamples(searchSubmissionId);
    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    final String json = ow.writeValueAsString(mongoFileUpload);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=result.json")
        .body(json);
  }

  @GetMapping(
      value = "/submissions",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<List<MongoFileUpload>> getSubmissions(
      @RequestHeader("Authorization") final String token) {
    final AuthToken authToken =
        accessControlService
            .extractToken(token)
            .orElseThrow(GlobalExceptions.AccessControlException::new);
    final String user = accessControlService.getUser(authToken);
    final List<MongoFileUpload> uploads = fileUploadService.getUserSubmissions(user);

    return ResponseEntity.ok().body(uploads);
  }

  private boolean isFileSizeExceeded(final MultipartFile file) throws RuntimeException {
    final long sizeBytes = file.getSize();
    final long sizeBytesKb = sizeBytes / 1000;

    return sizeBytesKb >= bioSamplesProperties.getBiosamplesFileUploaderMaxSameTimeUploadFileSize();
  }
}
