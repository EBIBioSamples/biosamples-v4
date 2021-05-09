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

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.hateoas.*;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.auth.SubmissionAccount;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.service.*;
import uk.ac.ebi.biosamples.solr.repo.CursorArrayList;
import uk.ac.ebi.biosamples.utils.LinkUtils;

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

  private static final String NCBI_IMPORT_DOMAIN = "self.BiosampleImportNCBI";
  private static final String ENA_IMPORT_DOMAIN = "self.BiosampleImportENA";

  private Logger log = LoggerFactory.getLogger(getClass());

  public SamplesRestController(
      SamplePageService samplePageService,
      FilterService filterService,
      BioSamplesAapService bioSamplesAapService,
      BioSamplesWebinAuthenticationService bioSamplesWebinAuthenticationService,
      SampleResourceAssembler sampleResourceAssembler,
      SampleManipulationService sampleManipulationService,
      SampleService sampleService,
      BioSamplesProperties bioSamplesProperties,
      SchemaValidationService schemaValidationService) {
    this.samplePageService = samplePageService;
    this.filterService = filterService;
    this.bioSamplesAapService = bioSamplesAapService;
    this.bioSamplesWebinAuthenticationService = bioSamplesWebinAuthenticationService;
    this.sampleResourceAssembler = sampleResourceAssembler;
    this.sampleManipulationService = sampleManipulationService;
    this.sampleService = sampleService;
    this.schemaValidationService = schemaValidationService;
    this.bioSamplesProperties = bioSamplesProperties;
  }

  // must return a ResponseEntity so that cache headers can be set
  @CrossOrigin(methods = RequestMethod.GET)
  @GetMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Resources<Resource<Sample>>> searchHal(
      @RequestParam(name = "text", required = false) String text,
      @RequestParam(name = "filter", required = false) String[] filter,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "page", required = false) final Integer page,
      @RequestParam(name = "size", required = false) final Integer size,
      @RequestParam(name = "sort", required = false) final String[] sort,
      @RequestParam(name = "curationrepo", required = false) final String curationRepo) {

    // Need to decode the %20 and similar from the parameters
    // this is *not* needed for the html controller
    String decodedText = LinkUtils.decodeText(text);
    String[] decodedFilter = LinkUtils.decodeTexts(filter);
    String decodedCursor = LinkUtils.decodeText(cursor);

    int effectivePage;

    if (page == null) {
      effectivePage = 0;
    } else {
      effectivePage = page;
    }

    int effectiveSize;

    if (size == null) {
      effectiveSize = 20;
    } else {
      effectiveSize = size;
    }

    Collection<Filter> filters = filterService.getFiltersCollection(decodedFilter);
    Collection<String> domains = bioSamplesAapService.getDomains();

    // Note - EBI load balancer does cache but doesn't add age header, so clients could cache up
    // to
    // twice this age
    CacheControl cacheControl =
        CacheControl.maxAge(
            bioSamplesProperties.getBiosamplesCorePageCacheMaxAge(), TimeUnit.SECONDS);
    // if the user has access to any domains, then mark the response as private as must be using
    // AAP
    // and responses will be different
    if (domains.size() > 0) {
      cacheControl.cachePrivate();
    }

    if (cursor != null) {
      log.trace("This cursor = " + decodedCursor);
      CursorArrayList<Sample> samples =
          samplePageService.getSamplesByText(
              decodedText, filters, domains, decodedCursor, effectiveSize, curationRepo);
      log.trace("Next cursor = " + samples.getNextCursorMark());

      Resources<Resource<Sample>> resources =
          new Resources<>(
              samples.stream()
                  .map(
                      s ->
                          s != null
                              ? sampleResourceAssembler.toResource(s, SampleRestController.class)
                              : null)
                  .collect(Collectors.toList()));

      resources.add(
          getCursorLink(
              decodedText,
              decodedFilter,
              decodedCursor,
              effectiveSize,
              Link.REL_SELF,
              this.getClass()));
      // only display the next link if there is a next cursor to go to
      if (!LinkUtils.decodeText(samples.getNextCursorMark()).equals(decodedCursor)
          && !samples.getNextCursorMark().equals("*")) {
        resources.add(
            getCursorLink(
                decodedText,
                decodedFilter,
                samples.getNextCursorMark(),
                effectiveSize,
                Link.REL_NEXT,
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
          new Sort(Arrays.stream(effectiveSort).map(this::parseSort).collect(Collectors.toList()));
      Pageable pageable = new PageRequest(effectivePage, effectiveSize, pageSort);
      Page<Sample> pageSample =
          samplePageService.getSamplesByText(text, filters, domains, pageable, curationRepo);
      Resources<Resource<Sample>> resources =
          populateResources(
              pageSample, effectiveSize, effectivePage, decodedText, decodedFilter, sort);

      return ResponseEntity.ok().cacheControl(cacheControl).body(resources);
    }
  }

  private Resources<Resource<Sample>> populateResources(
      Page<Sample> pageSample,
      int effectiveSize,
      int effectivePage,
      String decodedText,
      String[] decodedFilter,
      String[] sort) {
    PageMetadata pageMetadata =
        new PageMetadata(
            effectiveSize,
            pageSample.getNumber(),
            pageSample.getTotalElements(),
            pageSample.getTotalPages());
    Resources<Resource<Sample>> resources =
        new PagedResources<>(
            pageSample.getContent().stream()
                .map(
                    s ->
                        s != null
                            ? sampleResourceAssembler.toResource(s, SampleRestController.class)
                            : null)
                .collect(Collectors.toList()),
            pageMetadata);

    // if theres more than one page, link to first and last
    if (pageSample.getTotalPages() > 1) {
      resources.add(
          getPageLink(
              decodedText, decodedFilter, 0, effectiveSize, sort, Link.REL_FIRST, this.getClass()));
    }
    // if there was a previous page, link to it
    if (effectivePage > 0) {
      resources.add(
          getPageLink(
              decodedText,
              decodedFilter,
              effectivePage - 1,
              effectiveSize,
              sort,
              Link.REL_PREVIOUS,
              this.getClass()));
    }

    resources.add(
        getPageLink(
            decodedText,
            decodedFilter,
            effectivePage,
            effectiveSize,
            sort,
            Link.REL_SELF,
            this.getClass()));

    // if there is a next page, link to it
    if (effectivePage < pageSample.getTotalPages() - 1) {
      resources.add(
          getPageLink(
              decodedText,
              decodedFilter,
              effectivePage + 1,
              effectiveSize,
              sort,
              Link.REL_NEXT,
              this.getClass()));
    }
    // if theres more than one page, link to first and last
    if (pageSample.getTotalPages() > 1) {
      resources.add(
          getPageLink(
              decodedText,
              decodedFilter,
              pageSample.getTotalPages(),
              effectiveSize,
              sort,
              Link.REL_LAST,
              this.getClass()));
    }
    // if we are on the first page and not sorting
    if (effectivePage == 0 && (sort == null || sort.length == 0)) {
      resources.add(
          getCursorLink(decodedText, decodedFilter, "*", effectiveSize, "cursor", this.getClass()));
    }
    // if there is no search term, and on first page, add a link to use search
    // TODO
    //			if (text.trim().length() == 0 && page == 0) {
    //				resources.add(LinkUtils.cleanLink(ControllerLinkBuilder
    //					.linkTo(ControllerLinkBuilder.methodOn(SamplesRestController.class)
    //						.searchHal(null, filter, null, page, effectiveSize, sort, null))
    //					.withRel("search")));
    //			}

    resources.add(
        SampleAutocompleteRestController.getLink(decodedText, decodedFilter, null, "autocomplete"));

    UriComponentsBuilder uriComponentsBuilder =
        ControllerLinkBuilder.linkTo(SamplesRestController.class).toUriComponentsBuilder();
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
      String text, String[] filter, String cursor, int size, String rel, Class controllerClass) {
    UriComponentsBuilder builder =
        ControllerLinkBuilder.linkTo(controllerClass).toUriComponentsBuilder();

    if (text != null && text.trim().length() > 0) {
      builder.queryParam("text", text);
    }

    if (filter != null) {
      for (String filterString : filter) {
        builder.queryParam("filter", filterString);
      }
    }

    builder.queryParam("cursor", cursor);
    builder.queryParam("size", size);
    return new Link(builder.toUriString(), rel);
  }

  public static Link getPageLink(
      String text,
      String[] filter,
      int page,
      int size,
      String[] sort,
      String rel,
      Class controllerClass) {
    UriComponentsBuilder builder =
        ControllerLinkBuilder.linkTo(controllerClass).toUriComponentsBuilder();

    if (text != null && text.trim().length() > 0) {
      builder.queryParam("text", text);
    }

    if (filter != null) {
      for (String filterString : filter) {
        builder.queryParam("filter", filterString);
      }
    }

    builder.queryParam("page", page);
    builder.queryParam("size", size);

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
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
  @RequestMapping("/accession")
  public ResponseEntity<Object> accessionSample(
      HttpServletRequest request,
      @RequestBody Sample sample,
      @RequestParam(name = "preAccessioning", required = false, defaultValue = "false")
          final boolean preAccessioning,
      @RequestParam(name = "authProvider", required = false, defaultValue = "AAP")
          String authProvider) {
    log.debug("Received POST for accessioning " + sample);
    if (sample.hasAccession()) throw new SampleWithAccessionSumbissionException();

    if (authProvider.equalsIgnoreCase("WEBIN")) {
      final BearerTokenExtractor bearerTokenExtractor = new BearerTokenExtractor();
      final Authentication authentication = bearerTokenExtractor.extract(request);
      final SubmissionAccount webinAccount =
          bioSamplesWebinAuthenticationService
              .getWebinSubmissionAccount(String.valueOf(authentication.getPrincipal()))
              .getBody();

      sample = bioSamplesWebinAuthenticationService.handleWebinUser(sample, webinAccount.getId());
    } else {
      sample = bioSamplesAapService.handleSampleDomain(sample);
    }

    final Instant release =
        Instant.ofEpochSecond(
            LocalDateTime.now(ZoneOffset.UTC).plusYears(100).toEpochSecond(ZoneOffset.UTC));
    final Instant update = Instant.now();
    final SubmittedViaType submittedVia =
        sample.getSubmittedVia() == null ? SubmittedViaType.JSON_API : sample.getSubmittedVia();

    sample =
        Sample.Builder.fromSample(sample)
            .withRelease(release)
            .withUpdate(update)
            .withSubmittedVia(submittedVia)
            .build();

    /*Pre accessioning is done by other archives to get a BioSamples accession before processing their own pipelines. It is better to check duplicates in pre-accessioning cases
     * The original case of accession remains unchanged*/
    if (preAccessioning) {
      if (sampleService.searchSampleByDomainAndName(sample.getDomain(), sample.getName())) {
        return new ResponseEntity<>(
            "Sample already exists, use POST only for new submissions", HttpStatus.BAD_REQUEST);
      } else {
        sample = sampleService.store(sample, false, authProvider);
        final Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);

        return ResponseEntity.ok(sampleResource.getContent().getAccession());
      }
    } else {
      sample = sampleService.store(sample, false, authProvider);
      final Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample);

      return ResponseEntity.created(URI.create(sampleResource.getLink("self").getHref()))
          .body(sampleResource);
    }
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Resource<Sample>> post(
      HttpServletRequest request,
      @RequestBody Sample sample,
      @RequestParam(name = "setfulldetails", required = false, defaultValue = "true")
          boolean setFullDetails,
      @RequestParam(name = "authProvider", required = false, defaultValue = "AAP")
          String authProvider) {
    log.debug("Received POST for " + sample);

    if (authProvider.equalsIgnoreCase("WEBIN")) {
      final BearerTokenExtractor bearerTokenExtractor = new BearerTokenExtractor();

      if (sample.hasAccession()) {
        throw new SampleWithAccessionSumbissionException();
      }

      final Authentication authentication = bearerTokenExtractor.extract(request);
      final SubmissionAccount webinAccount =
          bioSamplesWebinAuthenticationService
              .getWebinSubmissionAccount(String.valueOf(authentication.getPrincipal()))
              .getBody();

      final String webinAccountId = webinAccount.getId();

      sample = bioSamplesWebinAuthenticationService.handleWebinUser(sample, webinAccountId);

      final Set<AbstractData> structuredData = sample.getData();

      if (structuredData != null && structuredData.size() > 0) {
        sample =
            bioSamplesWebinAuthenticationService.handleStructuredDataWebinUserInData(
                sample, webinAccountId);
      }
    } else {
      if (sample.hasAccession() && !bioSamplesAapService.isWriteSuperUser()) {
        // Throw an error only if the user is not a super user and is trying to post a sample
        // with an
        // accession
        throw new SampleWithAccessionSumbissionException();
      }

      sample = bioSamplesAapService.handleSampleDomain(sample);

      final Set<AbstractData> structuredData = sample.getData();

      if (!(bioSamplesAapService.isWriteSuperUser()
          || bioSamplesAapService.isIntegrationTestUser())) {
        if (structuredData != null && structuredData.size() > 0) {
          sample = bioSamplesAapService.handleStructuredDataDomainInData(sample);
        }
      }
    }

    // update, create date are system generated fields
    SubmittedViaType submittedVia =
        sample.getSubmittedVia() == null ? SubmittedViaType.JSON_API : sample.getSubmittedVia();

    sample =
        Sample.Builder.fromSample(sample)
            .withCreate(defineCreateDate(sample))
            .withSubmitted(defineSubmittedDate(sample))
            .withUpdate(Instant.now())
            .withSubmittedVia(submittedVia)
            .build();

    // Dont validate superuser samples, this helps to submit external (eg. NCBI, ENA) samples
    // Validate all samples submitted with WEBIN AUTH

    if (sample.getWebinSubmissionAccountId() != null) {
      schemaValidationService.validate(sample);
    } else if (!bioSamplesAapService.isWriteSuperUser()) {
      schemaValidationService.validate(sample);
    }

    if (!setFullDetails) {
      sample = sampleManipulationService.removeLegacyFields(sample);
    }

    sample = sampleService.store(sample, false, authProvider);

    // assemble a resource to return
    Resource<Sample> sampleResource = sampleResourceAssembler.toResource(sample, this.getClass());
    // create the response object with the appropriate status
    // TODO work out how to avoid using ResponseEntity but also set location header
    return ResponseEntity.created(URI.create(sampleResource.getLink("self").getHref()))
        .body(sampleResource);
  }

  private Instant defineCreateDate(final Sample sample) {
    final Instant now = Instant.now();

    return (sample.getDomain() != null && isNcbiOrEnaPipelineImportDomain(sample))
        ? (sample.getCreate() != null ? sample.getCreate() : now)
        : now;
  }

  private Instant defineSubmittedDate(final Sample sample) {
    final Instant now = Instant.now();

    return (sample.getDomain() != null && isNcbiOrEnaPipelineImportDomain(sample))
        ? (sample.getSubmitted() != null ? sample.getSubmitted() : now)
        : now;
  }

  private boolean isNcbiOrEnaPipelineImportDomain(Sample sample) {
    return sample.getDomain().equalsIgnoreCase(NCBI_IMPORT_DOMAIN)
        || sample.getDomain().equalsIgnoreCase(ENA_IMPORT_DOMAIN);
  }

  @ResponseStatus(
      value = HttpStatus.BAD_REQUEST,
      reason = "New sample submission should not contain an accession")
  // 400
  public static class SampleWithAccessionSumbissionException extends RuntimeException {}
}
