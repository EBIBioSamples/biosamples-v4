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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;
import uk.ac.ebi.biosamples.mongo.model.MongoFileUpload;
import uk.ac.ebi.biosamples.security.model.AuthRealm;
import uk.ac.ebi.biosamples.security.model.AuthRequestWebin;
import uk.ac.ebi.biosamples.security.model.AuthToken;
import uk.ac.ebi.biosamples.security.model.FileUploaderAuthRequest;
import uk.ac.ebi.biosamples.security.service.AccessControlService;
import uk.ac.ebi.biosamples.service.upload.FileUploadService;
import uk.ac.ebi.biosamples.service.upload.JsonSchemaStoreSchemaRetrievalService;

@Controller
@RequestMapping("/login")
public class FileUploadLoginController {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final JsonSchemaStoreSchemaRetrievalService jsonSchemaStoreSchemaRetrievalService;
  private final FileUploadService fileUploadService;
  private final BioSamplesProperties bioSamplesProperties;
  private final AccessControlService accessControlService;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  public FileUploadLoginController(
      final JsonSchemaStoreSchemaRetrievalService jsonSchemaStoreSchemaRetrievalService,
      final AccessControlService accessControlService,
      final FileUploadService fileUploadService,
      final BioSamplesProperties bioSamplesProperties) {
    this.jsonSchemaStoreSchemaRetrievalService = jsonSchemaStoreSchemaRetrievalService;
    this.accessControlService = accessControlService;
    this.fileUploadService = fileUploadService;
    this.bioSamplesProperties = bioSamplesProperties;
    this.restTemplate = new RestTemplate();
    this.objectMapper = new ObjectMapper();
  }

  @SneakyThrows
  @PreAuthorize("permitAll()")
  @PostMapping(value = "/auth")
  public String auth(
      @ModelAttribute("fileUploaderAuthRequest")
          final FileUploaderAuthRequest fileUploaderAuthRequest,
      final ModelMap model) {
    try {
      Map<String, String> checklists = new TreeMap<>();

      try {
        checklists = jsonSchemaStoreSchemaRetrievalService.getChecklists();
      } catch (final Exception e) {
        log.info("Couldn't get checklists from JSON schema store", e);
      }

      final AuthRequestWebin authRequestWebin =
          new AuthRequestWebin(
              fileUploaderAuthRequest.getUserName(),
              fileUploaderAuthRequest.getPassword(),
              Collections.singletonList(AuthRealm.ENA));
      final String token =
          getWebinAuthenticationToken(objectMapper.writeValueAsString(authRequestWebin)).getBody();
      final Optional<AuthToken> authToken = accessControlService.extractToken(token);
      final String webinSubmissionAccountId;

      if (authToken.isPresent()) {
        webinSubmissionAccountId = authToken.get().getUser();
      } else {
        throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
      }

      if (webinSubmissionAccountId == null) {
        throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
      }

      log.info("WEBIN token is " + token);

      model.addAttribute("loginmethod", "WEBIN");
      model.addAttribute("checklists", checklists);
      model.addAttribute("webinId", webinSubmissionAccountId);
      model.addAttribute("token", token);
      model.remove("wrongCreds");

      try {
        fetchRecentSubmissions(model, token);
      } catch (final Exception e) {
        log.info("Failed to fetch recent submissions " + e.getMessage() + " caught");
      }

      return "upload";
    } catch (final Exception e) {
      log.info("Uploader login failed - Username or Password wrong");
    }

    model.addAttribute("wrongCreds", "wrongCreds");

    return "uploadLogin";
  }

  private void fetchRecentSubmissions(final ModelMap model, final String token) {
    try {
      final List<MongoFileUpload> uploads = getSubmissions(token);

      model.addAttribute("submissions", uploads);
    } catch (final Exception e) {
      log.info("Failed to fetch recent submissions " + e.getMessage());

      model.addAttribute("submissions", null);
      throw new RuntimeException(e);
    }
  }

  private List<MongoFileUpload> getSubmissions(final String token) {
    try {
      final AuthToken authToken =
          accessControlService
              .extractToken(token)
              .orElseThrow(GlobalExceptions.AccessControlException::new);
      final String user = accessControlService.getUser(authToken);

      return fileUploadService.getUserSubmissions(user);
    } catch (final Exception e) {
      log.info("Failed in fetch submissions in getSubmissions() " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  public ResponseEntity<String> getWebinAuthenticationToken(final String authRequest) {
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    final HttpEntity<String> entity = new HttpEntity<>(authRequest, headers);

    try {
      final ResponseEntity<String> responseEntity =
          restTemplate.exchange(
              bioSamplesProperties.getBiosamplesWebinAuthTokenUri(),
              HttpMethod.POST,
              entity,
              String.class);
      if (responseEntity.getStatusCode() == HttpStatus.OK) {
        return responseEntity;
      } else {
        return null;
      }
    } catch (final Exception e) {
      throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
    }
  }
}
