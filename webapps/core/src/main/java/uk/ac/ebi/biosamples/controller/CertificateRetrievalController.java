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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.service.certification.CertifyService;

@RestController
@RequestMapping(value = "/certificates", produces = MediaType.APPLICATION_JSON_VALUE)
public class CertificateRetrievalController {
  private Logger log = LoggerFactory.getLogger(getClass());
  @Autowired private CertifyService certifyService;
  final JsonParser jp = new JsonParser();

  @GetMapping(
      value = "/{certificateName}",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> getCertificate(@PathVariable String certificateName)
      throws IOException {
    log.info("Fetching certificate with name " + certificateName);

    JsonElement je = jp.parse(certifyService.getCertificateByCertificateName(certificateName));
    String prettyJsonString = getGson().toJson(je);

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\""
                + certifyService.getCertificateFileNameByCertificateName(certificateName)
                + "\"")
        .body(prettyJsonString);
  }

  @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<CollectionModel<EntityModel<String>>> getAllCertificates() {
    return ResponseEntity.ok()
        .body(
            new CollectionModel<>(
                certifyService.getAllCertificates().stream()
                    .map(certificate -> new EntityModel<>(getGson().toJson(jp.parse(certificate))))
                    .collect(Collectors.toList())));
  }

  public Gson getGson() {
    return new GsonBuilder().setPrettyPrinting().create();
  }
}
