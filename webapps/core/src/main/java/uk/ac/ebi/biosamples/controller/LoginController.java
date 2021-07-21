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

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.auth.AuthRealm;
import uk.ac.ebi.biosamples.model.auth.AuthRequest;
import uk.ac.ebi.biosamples.model.auth.AuthRequestWebin;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.service.upload.JsonSchemaStoreSchemaRetrievalService;
import uk.ac.ebi.tsc.aap.client.exception.UserNameOrPasswordWrongException;
import uk.ac.ebi.tsc.aap.client.security.BioSamplesTokenAuthenticationService;

@Controller
@RequestMapping("/login")
public class LoginController {
  private Logger log = LoggerFactory.getLogger(getClass());

  private final BioSamplesAapService bioSamplesAapService;
  private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  private final BioSamplesTokenAuthenticationService bioSamplesTokenAuthenticationService;
  private final JsonSchemaStoreSchemaRetrievalService jsonSchemaStoreSchemaRetrievalService;

  @Autowired ObjectMapper objectMapper;

  public LoginController(
      final BioSamplesAapService bioSamplesAapService,
      final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
      final BioSamplesTokenAuthenticationService bioSamplesTokenAuthenticationService,
      final JsonSchemaStoreSchemaRetrievalService jsonSchemaStoreSchemaRetrievalService) {
    this.bioSamplesAapService = bioSamplesAapService;
    this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
    this.bioSamplesTokenAuthenticationService = bioSamplesTokenAuthenticationService;
    this.jsonSchemaStoreSchemaRetrievalService = jsonSchemaStoreSchemaRetrievalService;
  }

  @SneakyThrows
  @PreAuthorize("permitAll()")
  @PostMapping(value = "/auth")
  public String auth(
      @ModelAttribute("authRequest") final AuthRequest authRequest,
      final ModelMap model,
      final HttpServletRequest req) {
    try {
      log.info("Login way is " + authRequest.getLoginWay());
      final List<String> checklists = jsonSchemaStoreSchemaRetrievalService.getChecklists();

      if (authRequest.getLoginWay().equals("WEBIN")) {
        final AuthRequestWebin authRequestWebin =
            new AuthRequestWebin(authRequest.getUserName(), authRequest.getPassword(), Arrays.asList(AuthRealm.ENA));
        final String token = bioSamplesWebinAuthenticationService
                .getWebinToken(objectMapper.writeValueAsString(authRequestWebin))
                .getBody();
        final SubmissionAccount submissionAccount =
            bioSamplesWebinAuthenticationService.getWebinSubmissionAccount(token).getBody();

        model.addAttribute("loginmethod", "WEBIN");
        model.addAttribute("certificates", checklists);
        model.addAttribute("webinAccount", submissionAccount.getId());
        model.addAttribute("token", token);
        model.remove("wrongCreds");

      } else {
        final String token = bioSamplesAapService.authenticate(authRequest.getUserName(), authRequest.getPassword());
        final Authentication authentication = bioSamplesTokenAuthenticationService.getAuthenticationFromToken(token);
        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(authentication);

        HttpSession session = req.getSession(true);
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, sc);

        if (token != null) {
          final List<String> domains = bioSamplesAapService.getDomains(token);

          model.addAttribute("loginmethod", null);
          model.addAttribute("domains", domains);
          model.addAttribute("webinAccount", null);
          model.addAttribute("certificates", checklists);
          model.addAttribute("token", token);
          model.remove("wrongCreds");


        }


        return "upload";
      }

      return "uploadLogin";
    } catch (final Exception e) {
      if (e instanceof UserNameOrPasswordWrongException) {
        model.addAttribute("wrongCreds", "wrongCreds");
        return "uploadLogin";
      } else {
        model.addAttribute("wrongCreds", "wrongCreds");
        return "uploadLogin";
      }
    }
  }
}
