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
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.auth.AuthRealm;
import uk.ac.ebi.biosamples.model.auth.AuthRequest;
import uk.ac.ebi.biosamples.model.auth.AuthRequestWebin;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;
import uk.ac.ebi.biosamples.mongo.model.MongoFileUpload;
import uk.ac.ebi.biosamples.service.security.AccessControlService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.service.upload.FileUploadService;
import uk.ac.ebi.biosamples.service.upload.JsonSchemaStoreSchemaRetrievalService;
import uk.ac.ebi.tsc.aap.client.exception.UserNameOrPasswordWrongException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/login")
public class LoginController {
  private Logger log = LoggerFactory.getLogger(getClass());

  private final BioSamplesAapService bioSamplesAapService;
  private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  private final JsonSchemaStoreSchemaRetrievalService jsonSchemaStoreSchemaRetrievalService;
  private final FileUploadService fileUploadService;
  private final AccessControlService accessControlService;

  @Autowired ObjectMapper objectMapper;

  public LoginController(
          final BioSamplesAapService bioSamplesAapService,
          final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
          final JsonSchemaStoreSchemaRetrievalService jsonSchemaStoreSchemaRetrievalService,
          final AccessControlService accessControlService,
          final FileUploadService fileUploadService) {
    this.bioSamplesAapService = bioSamplesAapService;
    this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
    this.jsonSchemaStoreSchemaRetrievalService = jsonSchemaStoreSchemaRetrievalService;
    this.accessControlService = accessControlService;
    this.fileUploadService = fileUploadService;
  }

  @SneakyThrows
  @PreAuthorize("permitAll()")
  @PostMapping(value = "/auth")
  public String auth(
          @ModelAttribute("authRequest") final AuthRequest authRequest,
          final ModelMap model) {
    try {
      log.info("Login way is " + authRequest.getLoginWay());
      final Map<String, String> checklists = jsonSchemaStoreSchemaRetrievalService.getChecklists();

      if (authRequest.getLoginWay().equals("WEBIN")) {
        final AuthRequestWebin authRequestWebin =
            new AuthRequestWebin(
                authRequest.getUserName(), authRequest.getPassword(), Collections.singletonList(AuthRealm.ENA));
        final String token =
            bioSamplesWebinAuthenticationService
                .getWebinToken(objectMapper.writeValueAsString(authRequestWebin))
                .getBody();
        final SubmissionAccount submissionAccount =
            bioSamplesWebinAuthenticationService.getWebinSubmissionAccount(token).getBody();

        model.addAttribute("loginmethod", "WEBIN");
        model.addAttribute("certificates", checklists);
        model.addAttribute("webinAccount", submissionAccount.getId());
        model.addAttribute("token", token);
        model.remove("wrongCreds");

        fetchRecentSubmissions(model, token);

        return "upload";
      } else {
        final String token =
            bioSamplesAapService.authenticate(authRequest.getUserName(), authRequest.getPassword());

        if (token != null) {
          final List<String> domains = bioSamplesAapService.getDomains(token);

          model.addAttribute("loginmethod", null);
          model.addAttribute("domains", domains);
          model.addAttribute("webinAccount", null);
          model.addAttribute("certificates", checklists);
          model.addAttribute("token", token);
          model.remove("wrongCreds");

          fetchRecentSubmissions(model, token);

          return "upload";
        }
      }

      return "uploadLogin";
    } catch (final Exception e) {
      if (e instanceof UserNameOrPasswordWrongException) {
        log.info("Uploader login failed - Username or Password wrong");
      } else {
        log.info("Uploader login failed - Undermined exception");
      }

      model.addAttribute("wrongCreds", "wrongCreds");

      return "uploadLogin";
    }
  }

  private void fetchRecentSubmissions(final ModelMap model, final String token) {
    final List<MongoFileUpload> uploads = getSubmissions(token);
    model.addAttribute("submissions", uploads);
  }

  private List<MongoFileUpload> getSubmissions(final String token) {
    final AuthToken authToken = accessControlService.extractToken(token);
    final List<String> userRoles = accessControlService.getUserRoles(authToken);

    return fileUploadService.getUserSubmissions(userRoles);
  }
}
