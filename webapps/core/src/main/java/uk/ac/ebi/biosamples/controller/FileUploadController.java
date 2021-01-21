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

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/upload")
public class FileUploadController {
  @PostMapping
  public void upload(@RequestParam("file") MultipartFile file) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", file);

    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

    RestTemplate restTemplate = new RestTemplate();

    log.info("Before submission");

    ResponseEntity<Object> response =
        restTemplate.postForEntity(
            "http://localhost:8082/biosamples/sampletab/api/v1/file/va",
            requestEntity,
            Object.class);

    log.info("Response is : " + response.getStatusCode());
    log.info("uploaded file " + file.getOriginalFilename());
  }

  /*
  // Test call with a main.
  public static void main(String[] a) throws IOException {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      MultiValueMap<String, Object> body
              = new LinkedMultiValueMap<>();

      body.add("file", getUserFileResource());

      HttpEntity<MultiValueMap<String, Object>> requestEntity
              = new HttpEntity<>(body, headers);

      RestTemplate restTemplate = new RestTemplate();

      log.info("Before submission");

      try {
          ResponseEntity<String> response = restTemplate.exchange("http://localhost:8082/biosamples/sampletab/api/v1/file/va",
                  HttpMethod.POST, requestEntity, String.class);

          log.info("Response is : " + response.getStatusCode());

      } catch (Exception e) {
          e.printStackTrace();
      }
  }

  public static Resource getUserFileResource() throws IOException {
      Path tempFile = Files.createTempFile("upload-test-file", ".txt");
      Files.write(tempFile, "some test content...\nline1\nline2".getBytes());
      System.out.println("uploading: " + tempFile);
      File file = tempFile.toFile();
      return new Resource(file);
  }*/
}
