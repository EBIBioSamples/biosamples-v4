/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.validation.Valid;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.biosamples.service.upload.FileUploadService;
import uk.ac.ebi.biosamples.service.upload.IsaTabUploadService;
import uk.ac.ebi.biosamples.service.upload.UploadInvalidException;

@Controller
@RequestMapping("/upload")
public class FileUploadController {
  private Logger log = LoggerFactory.getLogger(getClass());

  @Autowired FileUploadService fileUploadService;

  @Autowired IsaTabUploadService isaTabUploadService;

  @PostMapping
  public ResponseEntity<byte[]> upload(
      @RequestParam("file") MultipartFile file,
      @Valid String hiddenAapDomain,
      @Valid String hiddenCertificate,
      @Valid String webinAccount)
      throws IOException {
    try {
      final File downloadableFile =
          isaTabUploadService.upload(file, hiddenAapDomain, hiddenCertificate, webinAccount);
      final byte[] bytes = FileUtils.readFileToByteArray(downloadableFile);
      final HttpHeaders headers = setResponseHeadersSuccess(downloadableFile);

      return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    } catch (UploadInvalidException e) {
      log.info("File upload failure " + e.getMessage());
      final Path temp = Files.createTempFile("failure_result", ".txt");

      try (final BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
        writer.write(e.getMessage());
      }

      final File failedUploadMessageFile = temp.toFile();
      final byte[] bytes = FileUtils.readFileToByteArray(failedUploadMessageFile);
      final HttpHeaders headers = setResponseHeadersFailure(failedUploadMessageFile);
      return new ResponseEntity<>(bytes, headers, HttpStatus.BAD_REQUEST);
    }
  }

  private HttpHeaders setResponseHeadersSuccess(File file) {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(new MediaType("text", "csv"));
    httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());

    return httpHeaders;
  }

  private HttpHeaders setResponseHeadersFailure(File file) {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(new MediaType("text", "plain"));
    httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());

    return httpHeaders;
  }

  @GetMapping(value = "/downloadExampleFile")
  public ResponseEntity<byte[]> downloadExampleFile() throws IOException {
    final Path temp = Files.createTempFile("upload_example", ".tsv");
    final File pathFile = temp.toFile();

    FileUtils.copyInputStreamToFile(
        this.getClass().getClassLoader().getResourceAsStream("isa-example.tsv"), pathFile);
    final byte[] bytes = FileUtils.readFileToByteArray(pathFile);
    final HttpHeaders headers = setResponseHeadersSuccess(pathFile);

    return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
  }
}
