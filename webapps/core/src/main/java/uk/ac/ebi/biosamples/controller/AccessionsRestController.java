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

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.model.Accession;
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.service.AccessionsService;
import uk.ac.ebi.biosamples.service.security.AccessControlService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;

@RestController
@RequestMapping("/accessions")
@CrossOrigin
public class AccessionsRestController {
  private final BioSamplesAapService bioSamplesAapService;
  private final AccessionsService accessionsService;
  private final AccessControlService accessControlService;

  public AccessionsRestController(
      BioSamplesAapService bioSamplesAapService,
      AccessionsService accessionsService,
      AccessControlService accessControlService) {
    this.bioSamplesAapService = bioSamplesAapService;
    this.accessionsService = accessionsService;
    this.accessControlService = accessControlService;
  }

  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<CollectionModel<Accession>> getAccessions(
      @RequestParam(name = "text", required = false) String text,
      @RequestParam(name = "filter", required = false) String[] filter,
      @RequestParam(name = "page", required = false) final Integer page,
      @RequestParam(name = "size", required = false) final Integer size,
      @RequestHeader(name = "Authorization", required = false) final String token) {

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final boolean webinAuth =
        authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE);
    AuthorizationProvider authProvider =
        webinAuth ? AuthorizationProvider.WEBIN : AuthorizationProvider.AAP;

    String webinSubmissionAccountId = null;
    Collection<String> domains = null;

    if (webinAuth) {
      webinSubmissionAccountId = authToken.get().getUser();
    } else {
      domains = bioSamplesAapService.getDomains();
    }

    int effectiveSize = size == null ? 100 : size;
    int effectivePage = page == null ? 0 : page;

    Page<String> pageAccessions =
        accessionsService.getAccessions(
            text, filter, domains, webinSubmissionAccountId, effectivePage, effectiveSize);

    PagedModel.PageMetadata pageMetadata =
        new PagedModel.PageMetadata(
            effectiveSize,
            pageAccessions.getNumber(),
            pageAccessions.getTotalElements(),
            pageAccessions.getTotalPages());

    CollectionModel<Accession> resources =
        new PagedModel<>(
            pageAccessions.getContent().stream().map(Accession::build).collect(Collectors.toList()),
            pageMetadata);

    addRelLinks(
        pageAccessions, resources, text, filter, effectivePage, effectiveSize, authProvider.name());

    return ResponseEntity.ok().body(resources);
  }

  private void addRelLinks(
      Page<String> pageAccessions,
      CollectionModel<Accession> resources,
      String text,
      String[] filter,
      Integer effectivePage,
      Integer effectiveSize,
      String authProvider) {
    resources.add(
        SamplesRestController.getPageLink(
            text,
            filter,
            authProvider,
            Optional.empty(),
            effectivePage,
            effectiveSize,
            null,
            Link.REL_SELF.value(),
            this.getClass()));

    // if theres more than one page, link to first and last
    if (pageAccessions.getTotalPages() > 1) {
      resources.add(
          SamplesRestController.getPageLink(
              text,
              filter,
              authProvider,
              Optional.empty(),
              0,
              effectiveSize,
              null,
              Link.REL_FIRST.value(),
              this.getClass()));
      resources.add(
          SamplesRestController.getPageLink(
              text,
              filter,
              authProvider,
              Optional.empty(),
              pageAccessions.getTotalPages(),
              effectiveSize,
              null,
              Link.REL_LAST.value(),
              this.getClass()));
    }
    // if there was a previous page, link to it
    if (effectivePage > 0) {
      resources.add(
          SamplesRestController.getPageLink(
              text,
              filter,
              authProvider,
              Optional.empty(),
              effectivePage - 1,
              effectiveSize,
              null,
              Link.REL_PREVIOUS.value(),
              this.getClass()));
    }

    // if there is a next page, link to it
    if (effectivePage < pageAccessions.getTotalPages() - 1) {
      resources.add(
          SamplesRestController.getPageLink(
              text,
              filter,
              authProvider,
              Optional.empty(),
              effectivePage + 1,
              effectiveSize,
              null,
              Link.REL_NEXT.value(),
              this.getClass()));
    }
  }
}
