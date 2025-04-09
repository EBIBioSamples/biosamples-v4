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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.SubmittedViaType;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.core.model.structured.AbstractData;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;
import uk.ac.ebi.biosamples.exception.GlobalExceptions.PaginationException;
import uk.ac.ebi.biosamples.service.*;
import uk.ac.ebi.biosamples.service.WebinAuthenticationService;
import uk.ac.ebi.biosamples.service.taxonomy.TaxonomyClientService;
import uk.ac.ebi.biosamples.service.validation.SchemaValidationService;
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
  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final String SRA_ACCESSION = "SRA accession";
  private final SamplePageService samplePageService;
  private final SampleService sampleService;
  private final FilterService filterService;
  private final WebinAuthenticationService webinAuthenticationService;
  private final SampleManipulationService sampleManipulationService;
  private final BioSamplesProperties bioSamplesProperties;
  private final SampleResourceAssembler sampleResourceAssembler;
  private final SchemaValidationService schemaValidationService;
  private final TaxonomyClientService taxonomyClientService;

  public SamplesRestController(
      final SamplePageService samplePageService,
      final FilterService filterService,
      final WebinAuthenticationService webinAuthenticationService,
      final SampleResourceAssembler sampleResourceAssembler,
      final SampleManipulationService sampleManipulationService,
      final SampleService sampleService,
      final BioSamplesProperties bioSamplesProperties,
      final SchemaValidationService schemaValidationService,
      final TaxonomyClientService taxonomyClientService) {
    this.samplePageService = samplePageService;
    this.filterService = filterService;
    this.webinAuthenticationService = webinAuthenticationService;
    this.sampleResourceAssembler = sampleResourceAssembler;
    this.sampleManipulationService = sampleManipulationService;
    this.sampleService = sampleService;
    this.schemaValidationService = schemaValidationService;
    this.bioSamplesProperties = bioSamplesProperties;
    this.taxonomyClientService = taxonomyClientService;
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
      @RequestParam(name = "applyCurations", required = false, defaultValue = "true")
          final boolean applyCurations) {

    // Need to decode the %20 and similar from the parameters, this is *not* needed for the html
    // controller
    final Authentication loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final String principle = sampleService.getPrinciple(loggedInUser);

    final String decodedText = LinkUtils.decodeText(text);
    final String[] decodedFilter = LinkUtils.decodeTexts(filter);
    String decodedCursor = LinkUtils.decodeText(cursor);

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

    final Collection<Filter> filters = filterService.getFiltersCollection(decodedFilter);

    // Note - EBI load balancer does cache but doesn't add age header, so clients could cache up
    // to
    // twice this age
    final CacheControl cacheControl =
        CacheControl.maxAge(
            bioSamplesProperties.getBiosamplesCorePageCacheMaxAge(), TimeUnit.SECONDS);

    if (cursor != null) {
      log.trace("This cursor = " + decodedCursor);
      final CursorArrayList<Sample> samples =
          samplePageService.getSamplesByText(
              decodedText, filters, principle, decodedCursor, effectiveSize, applyCurations);
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
              applyCurations,
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
                applyCurations,
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
                applyCurations,
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
          samplePageService.getSamplesByText(text, filters, principle, pageable, applyCurations);
      final CollectionModel<EntityModel<Sample>> resources =
          populateResources(
              pageSample,
              effectiveSize,
              effectivePage,
              decodedText,
              decodedFilter,
              sort,
              applyCurations);

      return ResponseEntity.ok().cacheControl(cacheControl).body(resources);
    }
  }

  private CollectionModel<EntityModel<Sample>> populateResources(
      final Page<Sample> pageSample,
      final int effectiveSize,
      final int effectivePage,
      final String decodedText,
      final String[] decodedFilter,
      final String[] sort,
      final boolean applyCurations) {
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
              applyCurations,
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
      final boolean applyCurations,
      final String cursor,
      final int size,
      final String rel,
      final Class controllerClass) {
    final UriComponentsBuilder builder = getUriComponentsBuilder(text, filter, controllerClass);

    if (applyCurations) {
      builder.queryParam("applyCurations", "true");
    }

    builder.queryParam("cursor", cursor);
    builder.queryParam("size", size);
    return Link.of(builder.toUriString(), rel);
  }

  private static UriComponentsBuilder getUriComponentsBuilder(
      final String text, final String[] filter, final Class controllerClass) {
    final UriComponentsBuilder builder =
        WebMvcLinkBuilder.linkTo(controllerClass).toUriComponentsBuilder();

    if (text != null && !text.trim().isEmpty()) {
      builder.queryParam("text", text);
    }

    if (filter != null) {
      for (final String filterString : filter) {
        builder.queryParam("filter", filterString);
      }
    }

    return builder;
  }

  static Link getPageLink(
      final String text,
      final String[] filter,
      final int page,
      final int size,
      final String[] sort,
      final String rel,
      final Class controllerClass) {
    final UriComponentsBuilder builder = getUriComponentsBuilder(text, filter, controllerClass);

    builder.queryParam("page", page);
    builder.queryParam("size", size);

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
    schemaValidationService.validate(sample, null);

    return ResponseEntity.ok(sample);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(
      path = "/accession",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EntityModel<Sample>> accession(
      @RequestBody Sample sample, @RequestHeader(name = "Authorization") final String token) {
    final Authentication loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final String principle = sampleService.getPrinciple(loggedInUser);

    if (principle == null) {
      throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
    }

    final boolean sampleHasSRAAccessionInAttributes =
        sample.getAttributes() != null
            && sample.getAttributes().stream()
                .anyMatch(attribute -> attribute.getType().equalsIgnoreCase(SRA_ACCESSION));

    if (sample.hasAccession() || sample.hasSraAccession() || sampleHasSRAAccessionInAttributes) {
      throw new GlobalExceptions.SampleWithAccessionSubmissionException();
    }

    final boolean isWebinSuperUser = webinAuthenticationService.isWebinSuperUser(principle);

    sample =
        webinAuthenticationService.handleWebinUserSubmission(sample, principle, Optional.empty());

    sample = sampleService.buildPrivateSample(sample);
    sample = sampleService.persistSample(sample, null, isWebinSuperUser);

    final EntityModel<Sample> sampleResource = sampleResourceAssembler.toModel(sample);

    return ResponseEntity.created(URI.create(sampleResource.getLink("self").get().getHref()))
        .body(sampleResource);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EntityModel<Sample>> post(
      @RequestBody Sample sample,
      @RequestParam(name = "setfulldetails", required = false, defaultValue = "true")
          final boolean setFullDetails) {
    final Authentication loggedInUser = SecurityContextHolder.getContext().getAuthentication();
    final String principle = sampleService.getPrinciple(loggedInUser);

    if (principle == null) {
      throw new GlobalExceptions.WebinUserLoginUnauthorizedException();
    }

    log.debug("Received POST request for sample: {}", sample);

    // Validate for structured data presence
    final Set<AbstractData> structuredData = sample.getData();

    if (structuredData != null && !structuredData.isEmpty()) {
      throw new GlobalExceptions.SampleValidationException(
          "Sample contains structured data. Please submit structured data separately using the sample update PUT endpoint.");
    }

    // Determine if user is a super user and validate sample with accessions
    final boolean isWebinSuperUser = webinAuthenticationService.isWebinSuperUser(principle);
    final Optional<Sample> oldSample =
        sampleService.validateSampleWithAccessionsAgainstConditionsAndGetOldSample(
            sample, isWebinSuperUser);
    final Instant now = Instant.now();

    // Handle Webin submission
    sample = webinAuthenticationService.handleWebinUserSubmission(sample, principle, oldSample);

    // Build and update the sample with required attributes
    sample =
        Sample.Builder.fromSample(sample)
            .withCreate(sampleService.defineCreateDate(sample, isWebinSuperUser))
            .withSubmitted(sampleService.defineSubmittedDate(sample, isWebinSuperUser))
            .withUpdate(now)
            .withSubmittedVia(
                Optional.ofNullable(sample.getSubmittedVia()).orElse(SubmittedViaType.JSON_API))
            .build();

    sample = validateSample(sample, principle, isWebinSuperUser);

    // Optionally remove legacy fields
    if (!setFullDetails) {
      sample = sampleManipulationService.removeLegacyFields(sample);
    }

    // Persist sample and assemble response
    sample = sampleService.persistSample(sample, oldSample.orElse(null), isWebinSuperUser);

    final EntityModel<Sample> sampleResource = sampleResourceAssembler.toModel(sample, getClass());

    return ResponseEntity.created(URI.create(sampleResource.getLink("self").get().getHref()))
        .body(sampleResource);
  }

  private Sample validateSample(
      Sample sample, final String principle, final boolean isWebinSuperUser) {
    if (!isWebinSuperUser) {
      sample = schemaValidationService.validate(sample, principle);
      sample = taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(sample, true);
    }

    if (sample.getSubmittedVia() == SubmittedViaType.FILE_UPLOADER) {
      sample = schemaValidationService.validate(sample, principle);
    }

    return sample;
  }
}
