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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.auth.*;
import uk.ac.ebi.biosamples.mongo.model.MongoFileUpload;
import uk.ac.ebi.biosamples.service.security.AccessControlService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.service.upload.FileUploadService;
import uk.ac.ebi.biosamples.service.upload.JsonSchemaStoreSchemaRetrievalService;

@Controller
@RequestMapping("/login")
public class FileUploadLoginController {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  private final JsonSchemaStoreSchemaRetrievalService jsonSchemaStoreSchemaRetrievalService;
  private final FileUploadService fileUploadService;
  private final AccessControlService accessControlService;

  @Autowired ObjectMapper objectMapper;

  public FileUploadLoginController(
      final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
      final JsonSchemaStoreSchemaRetrievalService jsonSchemaStoreSchemaRetrievalService,
      final AccessControlService accessControlService,
      final FileUploadService fileUploadService) {
    this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
    this.jsonSchemaStoreSchemaRetrievalService = jsonSchemaStoreSchemaRetrievalService;
    this.accessControlService = accessControlService;
    this.fileUploadService = fileUploadService;
  }

  @SneakyThrows
  @PreAuthorize("permitAll()")
  @PostMapping(value = "/auth")
  public String auth(
      @ModelAttribute("authRequest") final AuthRequest authRequest, final ModelMap model) {
    try {
      log.info("Login way is " + authRequest.getLoginWay());
      Map<String, String> checklists = new TreeMap<>();

      try {
        checklists = jsonSchemaStoreSchemaRetrievalService.getChecklists();
      } catch (final Exception e) {
        log.info("Couldn't get checklists from JSON schema store", e);
      }

      if (authRequest.getLoginWay().equals("WEBIN")) {
        final AuthRequestWebin authRequestWebin =
            new AuthRequestWebin(
                authRequest.getUserName(),
                authRequest.getPassword(),
                Collections.singletonList(AuthRealm.ENA));
        final String token =
            bioSamplesWebinAuthenticationService
                .getWebinAuthenticationToken(objectMapper.writeValueAsString(authRequestWebin))
                .getBody();
        final Optional<AuthToken> authToken = accessControlService.extractToken(token);
        final String webinSubmissionAccountId;

        if (authToken.isPresent()) {
          webinSubmissionAccountId = authToken.get().getUser();
        } else {
          throw new GlobalExceptions.WebinTokenInvalidException();
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
      }

      return "uploadLogin";
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
      final List<String> userRoles = accessControlService.getUserRoles(authToken);

      return fileUploadService.getUserSubmissions(userRoles);
    } catch (final Exception e) {
      log.info("Failed in fetch submissions in getSubmissions() " + e.getMessage());
      throw new RuntimeException(e);
    }
  }
}
