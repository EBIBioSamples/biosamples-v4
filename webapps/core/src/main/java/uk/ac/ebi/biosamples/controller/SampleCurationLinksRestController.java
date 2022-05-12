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

import java.net.URI;
import java.time.Instant;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;
import uk.ac.ebi.biosamples.service.CurationLinkResourceAssembler;
import uk.ac.ebi.biosamples.service.CurationPersistService;
import uk.ac.ebi.biosamples.service.security.AccessControlService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.utils.mongo.CurationReadService;

@RestController
@RequestMapping("/samples/{accession}/curationlinks")
public class SampleCurationLinksRestController {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final CurationReadService curationReadService;
  private final CurationPersistService curationPersistService;
  private final CurationLinkResourceAssembler curationLinkResourceAssembler;
  private final BioSamplesAapService bioSamplesAapService;
  private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  private final AccessControlService accessControlService;

  public SampleCurationLinksRestController(
      CurationReadService curationReadService,
      CurationPersistService curationPersistService,
      CurationLinkResourceAssembler curationLinkResourceAssembler,
      BioSamplesAapService bioSamplesAapService,
      BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
      AccessControlService accessControlService) {
    this.curationReadService = curationReadService;
    this.curationPersistService = curationPersistService;
    this.curationLinkResourceAssembler = curationLinkResourceAssembler;
    this.bioSamplesAapService = bioSamplesAapService;
    this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
    this.accessControlService = accessControlService;
  }

  @CrossOrigin
  @GetMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<PagedModel<EntityModel<CurationLink>>> getCurationLinkPageJson(
      @PathVariable String accession,
      Pageable pageable,
      PagedResourcesAssembler<CurationLink> pageAssembler) {

    Page<CurationLink> page = curationReadService.getCurationLinksForSample(accession, pageable);

    // add the links to each individual sample on the page
    // also adds links to first/last/next/prev at the same time
    PagedModel<EntityModel<CurationLink>> pagedResources =
        pageAssembler.toModel(
            page,
            curationLinkResourceAssembler,
            WebMvcLinkBuilder.linkTo(
                    WebMvcLinkBuilder.methodOn(SampleCurationLinksRestController.class)
                        .getCurationLinkPageJson(accession, pageable, pageAssembler))
                .withSelfRel());

    return ResponseEntity.ok(pagedResources);
  }

  @CrossOrigin
  @GetMapping(
      value = "/{hash}",
      produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<EntityModel<CurationLink>> getCurationLinkJson(
      @PathVariable String accession, @PathVariable String hash) {

    CurationLink curationLink = curationReadService.getCurationLink(hash);
    EntityModel<CurationLink> resource = curationLinkResourceAssembler.toModel(curationLink);

    return ResponseEntity.ok(resource);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<EntityModel<CurationLink>> createCurationLinkJson(
      HttpServletRequest request,
      @PathVariable String accession,
      @RequestBody CurationLink curationLink,
      @RequestHeader(name = "Authorization") final String token) {

    log.info("Recieved POST for " + curationLink);
    final boolean webinAuth =
        accessControlService
            .extractToken(token)
            .map(t -> t.getAuthority() == AuthorizationProvider.WEBIN)
            .orElse(Boolean.FALSE);

    if (curationLink.getSample() == null) {
      // curationLink has no sample, use the one specified in the URL
    } else if (!curationLink.getSample().equals(accession)) {
      // points to a different sample, this is an error
      log.warn(
          "Attempted to POST a curation link on " + curationLink.getSample() + " to " + accession);
      throw new GlobalExceptions.SampleNotMatchException();
    }

    if (webinAuth) {
      final SubmissionAccount webinAccount =
          bioSamplesWebinAuthenticationService.getWebinSubmissionAccount(token).getBody();

      curationLink =
          bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
              curationLink, webinAccount.getId());

      curationLink =
          CurationLink.build(
              accession,
              curationLink.getCuration(),
              null,
              curationLink.getWebinSubmissionAccountId(),
              Instant.now());
    } else {
      curationLink =
          CurationLink.build(
              accession, curationLink.getCuration(), curationLink.getDomain(), null, Instant.now());
      curationLink = bioSamplesAapService.handleCurationLinkDomain(curationLink);
    }

    // now actually persist it
    curationLink = curationPersistService.store(curationLink);
    EntityModel<CurationLink> resource = curationLinkResourceAssembler.toModel(curationLink);

    // create the response object with the appropriate status
    return ResponseEntity.created(URI.create(resource.getLink("self").get().getHref()))
        .body(resource);
  }

  @CrossOrigin
  @DeleteMapping(value = "/{hash}")
  public ResponseEntity<?> deleteCurationLinkJson(
      @PathVariable String accession, @PathVariable String hash) {
    log.info("Received DELETE for curation link " + hash);
    CurationLink curationLink = curationReadService.getCurationLink(hash);
    log.info("Deleting curationLink " + curationLink);
    curationPersistService.delete(curationLink);
    return ResponseEntity.noContent().build();
  }
}
