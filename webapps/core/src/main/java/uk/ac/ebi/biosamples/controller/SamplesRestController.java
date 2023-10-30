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
  private static final String SRA_ACCESSION = "SRA accession";
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
      final SamplePageService samplePageService,
      final FilterService filterService,
      final BioSamplesAapService bioSamplesAapService,
      final BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
      final SampleResourceAssembler sampleResourceAssembler,
      final SampleManipulationService sampleManipulationService,
      final SampleService sampleService,
      final BioSamplesProperties bioSamplesProperties,
      final SchemaValidationService schemaValidationService,
      final TaxonomyClientService taxonomyClientService,
      final AccessControlService accessControlService) {
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
      @RequestParam(name = "text", required = false) final String text,
      @RequestParam(name = "filter", required = false) final String[] filter,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "page", required = false) final Integer page,
      @RequestParam(name = "size", required = false) final Integer size,
      @RequestParam(name = "sort", required = false) final String[] sort,
      @RequestParam(name = "curationdomain", required = false) final String[] curationdomain,
      @RequestHeader(name = "Authorization", required = false) final String token) {

    // Need to decode the %20 and similar from the parameters, this is *not* needed for the html
    // controller
    final String decodedText = LinkUtils.decodeText(text);
    final String[] decodedFilter = LinkUtils.decodeTexts(filter);
    String decodedCursor = LinkUtils.decodeText(cursor);
    final Optional<List<String>> decodedCurationDomains =
        LinkUtils.decodeTextsToArray(curationdomain);
    String webinSubmissionAccountId = null;
    Collection<String> domains = null;

    final int effectivePage = page == null || page < 0 ? 0 : page;
    final int effectiveSize = size == null || size < 1 ? 20 : size;
    if (effectivePage > 500 || effectiveSize > 200) {
      throw new PaginationException(); // solr degrades with high page and size params, use cursor
      // instead
    }

    if (cursor == null && page == null) { // cursor crawling is the default
      cursor = "*";
      decodedCursor = "*";
    }

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);

    final boolean webinAuth =
        authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE);

    if (webinAuth) {
      webinSubmissionAccountId = authToken.map(AuthToken::getUser).orElse(null);
    } else {
      domains = bioSamplesAapService.getDomains();
    }

    final Collection<Filter> filters = filterService.getFiltersCollection(decodedFilter);

    // Note - EBI load balancer does cache but doesn't add age header, so clients could cache up
    // to
    // twice this age
    final CacheControl cacheControl =
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
      final CursorArrayList<Sample> samples =
          samplePageService.getSamplesByText(
              decodedText,
              filters,
              domains != null && !domains.isEmpty() ? domains : Collections.emptySet(),
              webinSubmissionAccountId,
              decodedCursor,
              effectiveSize,
              decodedCurationDomains);
      log.trace("Next cursor = " + samples.getNextCursorMark());

      final CollectionModel<EntityModel<Sample>> resources =
          CollectionModel.of(
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
              IanaLinkRelations.SELF.value(),
              getClass()));
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
                IanaLinkRelations.NEXT.value(),
                getClass()));
      }

      if (cursor.equals("*")) {
        resources.add(
            getCursorLink(
                decodedText,
                decodedFilter,
                decodedCurationDomains,
                "*",
                effectiveSize,
                "cursor",
                getClass()));
      }

      final UriComponentsBuilder uriComponentsBuilder =
          WebMvcLinkBuilder.linkTo(SamplesRestController.class).toUriComponentsBuilder();
      // This is a bit of a hack, but best we can do for now...
      resources.add(
          Link.of(uriComponentsBuilder.build(true).toUriString() + "/{accession}", "sample"));

      // Note - EBI load balancer does cache but doesn't add age header, so clients could cache up
      // to twice this age
      return ResponseEntity.ok().cacheControl(cacheControl).body(resources);
    } else {

      /*if (sort == null) {
        // if there is no existing sort, sort by score then accession
        effectiveSort = new String[2];
        effectiveSort[0] = "score,desc";
        effectiveSort[1] = "id,asc";
      }*/

      final Sort pageSort =
          sort != null
              ? Sort.by(Arrays.stream(sort).map(this::parseSort).collect(Collectors.toList()))
              : Sort.unsorted();
      final Pageable pageable = PageRequest.of(effectivePage, effectiveSize, pageSort);
      final Page<Sample> pageSample =
          samplePageService.getSamplesByText(
              text, filters, domains, webinSubmissionAccountId, pageable, decodedCurationDomains);
      final CollectionModel<EntityModel<Sample>> resources =
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
      final Page<Sample> pageSample,
      final int effectiveSize,
      final int effectivePage,
      final String authProvider,
      final String decodedText,
      final String[] decodedFilter,
      final String[] sort,
      final Optional<List<String>> decodedCurationDomains) {
    final PagedModel.PageMetadata pageMetadata =
        new PagedModel.PageMetadata(
            effectiveSize,
            pageSample.getNumber(),
            pageSample.getTotalElements(),
            pageSample.getTotalPages());
    final CollectionModel<EntityModel<Sample>> resources =
        PagedModel.of(
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
              IanaLinkRelations.FIRST.value(),
              getClass()));
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
              IanaLinkRelations.PREVIOUS.value(),
              getClass()));
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
            IanaLinkRelations.SELF.value(),
            getClass()));

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
              getClass()));
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
              IanaLinkRelations.LAST.value(),
              getClass()));
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
              getClass()));
    }

    final UriComponentsBuilder uriComponentsBuilder =
        WebMvcLinkBuilder.linkTo(SamplesRestController.class).toUriComponentsBuilder();
    // This is a bit of a hack, but best we can do for now...
    resources.add(
        Link.of(uriComponentsBuilder.build(true).toUriString() + "/{accession}", "sample"));

    return resources;
  }

  private Order parseSort(final String sort) {
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
  private static Link getCursorLink(
      final String text,
      final String[] filter,
      final Optional<List<String>> decodedCurationDomains,
      final String cursor,
      final int size,
      final String rel,
      final Class controllerClass) {
    final UriComponentsBuilder builder =
        getUriComponentsBuilder(text, filter, decodedCurationDomains, controllerClass);

    builder.queryParam("cursor", cursor);
    builder.queryParam("size", size);
    return Link.of(builder.toUriString(), rel);
  }

  private static UriComponentsBuilder getUriComponentsBuilder(
      final String text,
      final String[] filter,
      final Optional<List<String>> decodedCurationDomains,
      final Class controllerClass) {
    final UriComponentsBuilder builder =
        WebMvcLinkBuilder.linkTo(controllerClass).toUriComponentsBuilder();

    if (text != null && text.trim().length() > 0) {
      builder.queryParam("text", text);
    }

    if (filter != null) {
      for (final String filterString : filter) {
        builder.queryParam("filter", filterString);
      }
    }

    if (decodedCurationDomains.isPresent()) {
      if (decodedCurationDomains.get().isEmpty()) {
        builder.queryParam("curationdomain", "");
      } else {
        for (final String d : decodedCurationDomains.get()) {
          builder.queryParam("curationdomain", d);
        }
      }
    }

    return builder;
  }

  static Link getPageLink(
      final String text,
      final String[] filter,
      final String authProvider,
      final Optional<List<String>> decodedCurationDomains,
      final int page,
      final int size,
      final String[] sort,
      final String rel,
      final Class controllerClass) {
    final UriComponentsBuilder builder =
        getUriComponentsBuilder(text, filter, decodedCurationDomains, controllerClass);

    builder.queryParam("page", page);
    builder.queryParam("size", size);
    builder.queryParam("authProvider", authProvider);

    if (sort != null) {
      for (final String sortString : sort) {
        builder.queryParam("sort", sortString);
      }
    }
    return Link.of(builder.toUriString(), rel);
  }

  @PostMapping(
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE})
  @RequestMapping("/validate")
  public ResponseEntity<Sample> validateSample(@RequestBody final Sample sample) {
    schemaValidationService.validate(sample);

    return ResponseEntity.ok(sample);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @RequestMapping("/accession")
  public ResponseEntity<EntityModel<Sample>> accession(
      @RequestBody Sample sample, @RequestHeader(name = "Authorization") final String token) {
    final boolean sampleHasSRAAccessionInAttributes =
        sample.getAttributes() != null
            && sample.getAttributes().stream()
                .anyMatch(attribute -> attribute.getType().equalsIgnoreCase(SRA_ACCESSION));

    boolean isWebinSuperUser = false;

    if (sample.hasAccession() || sampleHasSRAAccessionInAttributes) {
      throw new GlobalExceptions.SampleWithAccessionSubmissionException();
    }

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final AuthorizationProvider authProvider =
        authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE)
            ? AuthorizationProvider.WEBIN
            : AuthorizationProvider.AAP;

    if (authProvider == AuthorizationProvider.WEBIN) {
      final String webinSubmissionAccountId = authToken.get().getUser();

      if (webinSubmissionAccountId == null) {
        throw new GlobalExceptions.WebinTokenInvalidException();
      }

      isWebinSuperUser =
          bioSamplesWebinAuthenticationService.isWebinSuperUser(webinSubmissionAccountId);

      sample =
          bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
              sample, webinSubmissionAccountId, Optional.empty());
    } else {
      sample = bioSamplesAapService.handleSampleDomain(sample, Optional.empty());
    }

    sample = sampleService.buildPrivateSample(sample);
    sample = sampleService.persistSample(sample, null, authProvider, isWebinSuperUser);
    final EntityModel<Sample> sampleResource = sampleResourceAssembler.toModel(sample);

    return ResponseEntity.created(URI.create(sampleResource.getLink("self").get().getHref()))
        .body(sampleResource);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<EntityModel<Sample>> post(
      @RequestBody Sample sample,
      @RequestParam(name = "setfulldetails", required = false, defaultValue = "true")
          final boolean setFullDetails,
      @RequestHeader(name = "Authorization") final String token) {
    log.debug("Received POST for " + sample);

    // can't submit structured data with the sample
    final Set<AbstractData> structuredData = sample.getData();
    final Optional<Sample> oldSample;

    if (structuredData != null && !structuredData.isEmpty()) {
      throw new GlobalExceptions.SampleValidationException(
          "Sample contains structured data. Please submit structured data separately using the sample update PUT endpoint");
    }

    final Optional<AuthToken> authToken = accessControlService.extractToken(token);
    final AuthorizationProvider authProvider =
        authToken.map(t -> t.getAuthority() == AuthorizationProvider.WEBIN).orElse(Boolean.FALSE)
            ? AuthorizationProvider.WEBIN
            : AuthorizationProvider.AAP;
    boolean isWebinSuperUser = false;

    if (authProvider == AuthorizationProvider.WEBIN) {
      final String webinSubmissionAccountId = authToken.get().getUser();

      if (webinSubmissionAccountId == null) {
        throw new GlobalExceptions.WebinTokenInvalidException();
      }

      isWebinSuperUser =
          bioSamplesWebinAuthenticationService.isWebinSuperUser(webinSubmissionAccountId);

      oldSample =
          sampleService.validateSampleWithAccessionsAgainstConditionsAndGetOldSample(
              sample, isWebinSuperUser);

      sample =
          bioSamplesWebinAuthenticationService.handleWebinUserSubmission(
              sample, webinSubmissionAccountId, oldSample);
    } else {
      oldSample =
          sampleService.validateSampleWithAccessionsAgainstConditionsAndGetOldSample(
              sample, bioSamplesAapService.isWriteSuperUser());

      sample = bioSamplesAapService.handleSampleDomain(sample, oldSample);
    }

    // update, create date are system generated fields
    sample =
        Sample.Builder.fromSample(sample)
            .withCreate(sampleService.defineCreateDate(sample, isWebinSuperUser))
            .withSubmitted(sampleService.defineSubmittedDate(sample, isWebinSuperUser))
            .withUpdate(Instant.now())
            .withSubmittedVia(
                sample.getSubmittedVia() == null
                    ? SubmittedViaType.JSON_API
                    : sample.getSubmittedVia())
            .build();

    sample = validateSample(sample, authProvider, isWebinSuperUser);

    if (!setFullDetails) {
      sample = sampleManipulationService.removeLegacyFields(sample);
    }

    sample =
        sampleService.persistSample(sample, oldSample.orElse(null), authProvider, isWebinSuperUser);

    // assemble a resource to return
    final EntityModel<Sample> sampleResource = sampleResourceAssembler.toModel(sample, getClass());
    // create the response object with the appropriate status
    return ResponseEntity.created(URI.create(sampleResource.getLink("self").get().getHref()))
        .body(sampleResource);
  }

  private Sample validateSample(
      Sample sample,
      final AuthorizationProvider authorizationProvider,
      final boolean isWebinSuperUser) {
    // Dont validate superuser samples, this helps to submit external (eg. NCBI, ENA) samples
    final boolean isWebinAuth = authorizationProvider == AuthorizationProvider.WEBIN;

    if (isWebinAuth && !isWebinSuperUser) {
      schemaValidationService.validate(sample);
      sample = taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(sample, true);
    } else if (!isWebinAuth && !bioSamplesAapService.isWriteSuperUser()) {
      schemaValidationService.validate(sample);
      sample = taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(sample, false);
    }

    if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
      schemaValidationService.validate(sample);
    }

    return sample;
  }
}
