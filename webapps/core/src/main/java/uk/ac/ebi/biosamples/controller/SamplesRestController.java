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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions.PaginationException;
import uk.ac.ebi.biosamples.model.AuthToken;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.service.*;
import uk.ac.ebi.biosamples.service.security.AccessControlService;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.security.BioSamplesWebinAuthenticationService;
import uk.ac.ebi.biosamples.service.taxonomy.TaxonomyClientService;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;
import uk.ac.ebi.biosamples.utils.LinkUtils;
import uk.ac.ebi.biosamples.validation.SchemaValidationService;

/**
 * Primary controller for REST operations both in JSON and XML and both read and write.
 *
 * <p>See {@link SampleHtmlController} for the HTML equivalent controller.
 *
 * @author faulcon
 */
@RestController
@ExposesResourceFor(Sample.class)
@RequestMapping("/samples")
@CrossOrigin
public class SamplesRestController {
  private final SamplePageService samplePageService;
  private final SampleService sampleService;
  private final FilterService filterService;
  private final BioSamplesAapService bioSamplesAapService;
  private final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService;
  private final SampleManipulationService sampleManipulationService;
  private final BioSamplesProperties bioSamplesProperties;
  private final SampleResourceAssembler sampleResourceAssembler;
  private final SchemaValidationService schemaValidationService;
  private final TaxonomyClientService taxonomyClientService;
  private final AccessControlService accessControlService;

  private final Logger log = LoggerFactory.getLogger(getClass());

  public SamplesRestController(
      SamplePageService samplePageService,
      FilterService filterService,
      BioSamplesAapService bioSamplesAapService,
      BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
      SampleResourceAssembler sampleResourceAssembler,
      SampleManipulationService sampleManipulationService,
      SampleService sampleService,
      BioSamplesProperties bioSamplesProperties,
      SchemaValidationService schemaValidationService,
      TaxonomyClientService taxonomyClientService,
      AccessControlService accessControlService) {
    this.samplePageService = samplePageService;
    this.filterService = filterService;
    this.bioSamplesAapService = bioSamplesAapService;
    this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
    this.sampleResourceAssembler = sampleResourceAssembler;
    this.sampleManipulationService = sampleManipulationService;
    this.sampleService = sampleService;
    this.schemaValidationService = schemaValidationService;
    this.bioSamplesProperties = bioSamplesProperties;
    this.taxonomyClientService = taxonomyClientService;
    this.accessControlService = accessControlService;
  }

  // must return a ResponseEntity so that cache headers can be set
  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<CollectionModel<EntityModel<Sample>>> searchHal(
      @RequestParam(name = "text", required = false) String text,
      @RequestParam(name = "filter", required = false) String[] filter,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "page", required = false) final Integer page,
      @RequestParam(name = "size", required = false) final Integer size,
      @RequestParam(name = "sort", required = false) final String[] sort,
      @RequestParam(name = "curationrepo", required = false) final String curationRepo,
      @RequestParam(name = "curationdomain", required = false) String[] curationdomain,
      @RequestHeader(name = "Authorization", required = false) final String token) {
    // Need to decode the %20 and similar from the parameters
    // this is *not* needed for the html controller
    String decodedText = LinkUtils.decodeText(text);
    String[] decodedFilter = LinkUtils.decodeTexts(filter);
    String decodedCursor = LinkUtils.decodeText(cursor);
    Optional<List<String>> decodedCurationDomains = LinkUtils.decodeTextsToArray(curationdomain);
    String webinSubmissionAccountId = null;
    Collection<String> domains = null;

    int effectivePage = page == null || page < 0 ? 0 : page;
    int effectiveSize = size == null || size < 1 ? 20 : size;
    if (effectivePage > 500 || effectiveSize > 200) {
      throw new PaginationException(); // solr degrades with high page and size params, use cursor instead
    }

    if (cursor == null && effectivePage == 0) { // cursor crawling is the default
      cursor = "*";
    }

    Optional<AuthToken> authToken = accessControlService.extractToken(token);

    final boolean webinAuth =
        authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE);

    if (webinAuth) {
      webinSubmissionAccountId = authToken.map(AuthToken::getUser).orElse(null);
    } else {
      domains = bioSamplesAapService.getDomains();
    }

    Collection<Filter> filters = filterService.getFiltersCollection(decodedFilter);

    // Note - EBI load balancer does cache but doesn't add age header, so clients could cache up
    // to
    // twice this age
    CacheControl cacheControl =
        CacheControl.maxAge(
            bioSamplesProperties.getBiosamplesCorePageCacheMaxAge(), TimeUnit.SECONDS);
    // if the user has access to any domains, then mark the response as private as must be using
    // AAP
    // and responses will be different
    if (domains != null && !domains.isEmpty()) {
      cacheControl.cachePrivate();
    }

    if (cursor != null) {
      log.trace("This cursor = " + decodedCursor);
      CursorArrayList<Sample> samples =
          samplePageService.getSamplesByText(
              decodedText,
              filters,
              domains != null && !domains.isEmpty() ? domains : Collections.emptySet(),
              webinSubmissionAccountId,
              decodedCursor,
              effectiveSize,
              curationRepo,
              decodedCurationDomains);
      log.trace("Next cursor = " + samples.getNextCursorMark());

      CollectionModel<EntityModel<Sample>> resources =
          new CollectionModel<>(
              samples.stream()
                  .map(
                      s ->
                          s != null
                              ? sampleResourceAssembler.toModel(s, SampleRestController.class)
                              : null)
                  .collect(Collectors.toList()));

      resources.add(
          getCursorLink(
              decodedText,
              decodedFilter,
              decodedCurationDomains,
              decodedCursor,
              effectiveSize,
              Link.REL_SELF.value(),
              this.getClass()));
      // only display the next link if there is a next cursor to go to
      if (!LinkUtils.decodeText(samples.getNextCursorMark()).equals(decodedCursor)
          && !samples.getNextCursorMark().equals("*")) {
        resources.add(
            getCursorLink(
                decodedText,
                decodedFilter,
                decodedCurationDomains,
                samples.getNextCursorMark(),
                effectiveSize,
                Link.REL_NEXT.value(),
                this.getClass()));
      }

      // Note - EBI load balancer does cache but doesn't add age header, so clients could
      // cache up
      // to twice this age
      return ResponseEntity.ok().cacheControl(cacheControl).body(resources);
    } else {
      String[] effectiveSort = sort;

      if (sort == null) {
        // if there is no existing sort, sort by score then accession
        effectiveSort = new String[2];
        effectiveSort[0] = "score,desc";
        effectiveSort[1] = "id,asc";
      }

      Sort pageSort =
          Sort.by(Arrays.stream(effectiveSort).map(this::parseSort).collect(Collectors.toList()));
      Pageable pageable = PageRequest.of(effectivePage, effectiveSize, pageSort);
      Page<Sample> pageSample =
          samplePageService.getSamplesByText(
              text,
              filters,
              domains,
              webinSubmissionAccountId,
              pageable,
              curationRepo,
              decodedCurationDomains);
      CollectionModel<EntityModel<Sample>> resources =
          populateResources(
              pageSample,
              effectiveSize,
              effectivePage,
              webinAuth ? AuthorizationProvider.WEBIN.name() : AuthorizationProvider.AAP.name(),
              decodedText,
              decodedFilter,
              sort,
              decodedCurationDomains);

      return ResponseEntity.ok().cacheControl(cacheControl).body(resources);
    }
  }

  private CollectionModel<EntityModel<Sample>> populateResources(
      Page<Sample> pageSample,
      int effectiveSize,
      int effectivePage,
      String authProvider,
      String decodedText,
      String[] decodedFilter,
      String[] sort,
      Optional<List<String>> decodedCurationDomains) {
    PagedModel.PageMetadata pageMetadata =
        new PagedModel.PageMetadata(
            effectiveSize,
            pageSample.getNumber(),
            pageSample.getTotalElements(),
            pageSample.getTotalPages());
    CollectionModel<EntityModel<Sample>> resources =
        new PagedModel<>(
            pageSample.getContent().stream()
                .map(
                    s ->
                        s != null
                            ? sampleResourceAssembler.toModel(s, SampleRestController.class)
                            : null)
                .collect(Collectors.toList()),
            pageMetadata);

    // if there is more than one page, link to first and last
    if (pageSample.getTotalPages() > 1) {
      resources.add(
          getPageLink(
              decodedText,
              decodedFilter,
              authProvider,
              decodedCurationDomains,
              0,
              effectiveSize,
              sort,
              Link.REL_FIRST.value(),
              this.getClass()));
    }
    // if there was a previous page, link to it
    if (effectivePage > 0) {
      resources.add(
          getPageLink(
              decodedText,
              decodedFilter,
              authProvider,
              decodedCurationDomains,
              effectivePage - 1,
              effectiveSize,
              sort,
              Link.REL_PREVIOUS.value(),
              this.getClass()));
    }

    resources.add(
        getPageLink(
            decodedText,
            decodedFilter,
            authProvider,
            decodedCurationDomains,
            effectivePage,
            effectiveSize,
            sort,
            Link.REL_SELF.value(),
            this.getClass()));

    // if there is a next page, link to it
    if (effectivePage < pageSample.getTotalPages() - 1) {
      resources.add(
          getPageLink(
              decodedText,
              decodedFilter,
              authProvider,
              decodedCurationDomains,
              effectivePage + 1,
              effectiveSize,
              sort,
              IanaLinkRelations.NEXT.value(),
              this.getClass()));
    }
    // if theres more than one page, link to first and last
    if (pageSample.getTotalPages() > 1) {
      resources.add(
          getPageLink(
              decodedText,
              decodedFilter,
              authProvider,
              decodedCurationDomains,
              pageSample.getTotalPages(),
              effectiveSize,
              sort,
              Link.REL_LAST.value(),
              this.getClass()));
    }
    // if we are on the first page and not sorting
    if (effectivePage == 0 && (sort == null || sort.length == 0)) {
      resources.add(
          getCursorLink(
              decodedText,
              decodedFilter,
              decodedCurationDomains,
              "*",
              effectiveSize,
              "cursor",
              this.getClass()));
    }

    UriComponentsBuilder uriComponentsBuilder =
        WebMvcLinkBuilder.linkTo(SamplesRestController.class).toUriComponentsBuilder();
    // This is a bit of a hack, but best we can do for now...
    resources.add(
        new Link(uriComponentsBuilder.build(true).toUriString() + "/{accession}", "sample"));

    return resources;
  }

  private Order parseSort(String sort) {
    if (sort.endsWith(",desc")) {
      return new Order(Sort.Direction.DESC, sort.substring(0, sort.length() - 5));
    } else if (sort.endsWith(",asc")) {
      return new Order(Sort.Direction.ASC, sort.substring(0, sort.length() - 4));
    } else {
      return new Order(null, sort);
    }
  }

  /**
   * ControllerLinkBuilder seems to have problems linking to the same controller? Split out into
   * manual manipulation for greater control
   */
  public static Link getCursorLink(
      String text,
      String[] filter,
      Optional<List<String>> decodedCurationDomains,
      String cursor,
      int size,
      String rel,
      Class controllerClass) {
    UriComponentsBuilder builder =
        getUriComponentsBuilder(text, filter, decodedCurationDomains, controllerClass);

    builder.queryParam("cursor", cursor);
    builder.queryParam("size", size);
    return new Link(builder.toUriString(), rel);
  }

  private static UriComponentsBuilder getUriComponentsBuilder(
      String text,
      String[] filter,
      Optional<List<String>> decodedCurationDomains,
      Class controllerClass) {
    UriComponentsBuilder builder =
        WebMvcLinkBuilder.linkTo(controllerClass).toUriComponentsBuilder();

    if (text != null && text.trim().length() > 0) {
      builder.queryParam("text", text);
    }

    if (filter != null) {
      for (String filterString : filter) {
        builder.queryParam("filter", filterString);
      }
    }

    if (decodedCurationDomains.isPresent()) {
      if (decodedCurationDomains.get().isEmpty()) {
        builder.queryParam("curationdomain", "");
      } else {
        for (String d : decodedCurationDomains.get()) {
          builder.queryParam("curationdomain", d);
        }
      }
    }

    return builder;
  }

  public static Link getPageLink(
      String text,
      String[] filter,
      String authProvider,
      Optional<List<String>> decodedCurationDomains,
      int page,
      int size,
      String[] sort,
      String rel,
      Class controllerClass) {
    UriComponentsBuilder builder =
        getUriComponentsBuilder(text, filter, decodedCurationDomains, controllerClass);

    builder.queryParam("page", page);
    builder.queryParam("size", size);
    builder.queryParam("authProvider", authProvider);

    if (sort != null) {
      for (String sortString : sort) {
        builder.queryParam("sort", sortString);
      }
    }
    return new Link(builder.toUriString(), rel);
  }

  @PostMapping(
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE})
  @RequestMapping("/validate")
  public ResponseEntity<Sample> validateSample(@RequestBody Sample sample) {
    schemaValidationService.validate(sample);

    return ResponseEntity.ok(sample);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @RequestMapping("/accession")
  public ResponseEntity<EntityModel<Sample>> accessionSample(
      @RequestBody Sample sample, @RequestHeader(name = "Authorization") final String token) {
    boolean isWebinSuperUser = false;

    if (sample.hasAccession()) {
      throw new GlobalExceptions.SampleWithAccessionSubmissionException();
    }

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final boolean webinAuth =
        authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE);
    final AuthorizationProvider authProvider =
        webinAuth ? AuthorizationProvider.WEBIN : AuthorizationProvider.AAP;

    if (webinAuth) {
      final String webinSubmissionAccountId = authToken.get().getUser();

      if (webinSubmissionAccountId == null) {
        throw new GlobalExceptions.WebinTokenInvalidException();
      }

      isWebinSuperUser =
          bioSamplesWebinAuthenticationService.isWebinSuperUser(webinSubmissionAccountId);

      sample =
          bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
              sample, webinSubmissionAccountId);
    } else {
      sample = bioSamplesAapService.handleSampleDomain(sample);
    }

    sample = sampleService.buildPrivateSample(sample);
    sample = sampleService.persistSample(sample, authProvider, isWebinSuperUser);
    final EntityModel<Sample> sampleResource = sampleResourceAssembler.toModel(sample);

    return ResponseEntity.created(URI.create(sampleResource.getLink("self").get().getHref()))
        .body(sampleResource);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<EntityModel<Sample>> post(
      @RequestBody Sample sample,
      @RequestParam(name = "setfulldetails", required = false, defaultValue = "true")
          boolean setFullDetails,
      @RequestHeader(name = "Authorization") final String token) {
    log.debug("Received POST for " + sample);

    // can't submit structured data with the sample
    final Set<AbstractData> structuredData = sample.getData();

    if (structuredData != null && !structuredData.isEmpty()) {
      throw new GlobalExceptions.SampleValidationException(
          "Sample contains structured data. Please submit structured data separately using the sample update PUT endpoint");
    }

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final boolean webinAuth =
        authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE);
    final AuthorizationProvider authProvider =
        webinAuth ? AuthorizationProvider.WEBIN : AuthorizationProvider.AAP;
    boolean isWebinSuperUser = false;

    if (webinAuth) {
      final String webinSubmissionAccountId = authToken.get().getUser();

      if (webinSubmissionAccountId == null) {
        throw new GlobalExceptions.WebinTokenInvalidException();
      }

      isWebinSuperUser =
          bioSamplesWebinAuthenticationService.isWebinSuperUser(webinSubmissionAccountId);

      if (sample.hasAccession() && !isWebinSuperUser) {
        throw new GlobalExceptions.SampleWithAccessionSubmissionException();
      }

      sample =
          bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
              sample, webinSubmissionAccountId);
    } else {
      if (sample.hasAccession() && !bioSamplesAapService.isWriteSuperUser()) {
        throw new GlobalExceptions.SampleWithAccessionSubmissionException();
      }

      sample = bioSamplesAapService.handleSampleDomain(sample);
    }

    // update, create date are system generated fields
    SubmittedViaType submittedVia =
        sample.getSubmittedVia() == null ? SubmittedViaType.JSON_API : sample.getSubmittedVia();

    sample =
        Sample.Builder.fromSample(sample)
            .withCreate(sampleService.defineCreateDate(sample))
            .withSubmitted(sampleService.defineSubmittedDate(sample))
            .withUpdate(Instant.now())
            .withSubmittedVia(submittedVia)
            .build();

    sample = validateSample(sample, webinAuth, isWebinSuperUser);

    if (!setFullDetails) {
      sample = sampleManipulationService.removeLegacyFields(sample);
    }

    sample = sampleService.persistSample(sample, authProvider, isWebinSuperUser);

    // assemble a resource to return
    EntityModel<Sample> sampleResource = sampleResourceAssembler.toModel(sample, this.getClass());
    // create the response object with the appropriate status
    return ResponseEntity.created(URI.create(sampleResource.getLink("self").get().getHref()))
        .body(sampleResource);
  }

  private Sample validateSample(Sample sample, boolean webinAuth, boolean isWebinSuperUser) {
    // Dont validate superuser samples, this helps to submit external (eg. NCBI, ENA) samples
    if (webinAuth && !isWebinSuperUser) {
      schemaValidationService.validate(sample);
      sample = taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(sample, true);
    } else if (!webinAuth && !bioSamplesAapService.isWriteSuperUser()) {
      schemaValidationService.validate(sample);
      sample = taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(sample, false);
    }

    if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
      schemaValidationService.validate(sample);
    }

    return sample;
  }
}
