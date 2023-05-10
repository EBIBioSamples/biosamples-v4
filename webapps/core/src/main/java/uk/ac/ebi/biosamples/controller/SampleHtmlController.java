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

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions.PaginationException;
import uk.ac.ebi.biosamples.model.JsonLDDataCatalog;
import uk.ac.ebi.biosamples.model.JsonLDDataset;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.auth.AuthorizationProvider;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.*;
import uk.ac.ebi.biosamples.service.security.BioSamplesAapService;

/**
 * Primary controller for HTML operations.
 *
 * <p>See {@link SampleRestController} for the equivalent REST controller.
 *
 * @author faulcon
 */
@Controller
@RequestMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
public class SampleHtmlController {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final SampleService sampleService;
  private final SamplePageService samplePageService;
  private final JsonLDService jsonLDService;
  private final FacetService facetService;
  private final FilterService filterService;
  private final BioSamplesAapService bioSamplesAapService;
  private final BioSamplesProperties bioSamplesProperties;

  public SampleHtmlController(
      final SampleService sampleService,
      final SamplePageService samplePageService,
      final JsonLDService jsonLDService,
      final FacetService facetService,
      final FilterService filterService,
      final BioSamplesAapService bioSamplesAapService,
      final BioSamplesProperties bioSamplesProperties) {
    this.sampleService = sampleService;
    this.samplePageService = samplePageService;
    this.jsonLDService = jsonLDService;
    this.facetService = facetService;
    this.filterService = filterService;
    this.bioSamplesAapService = bioSamplesAapService;
    this.bioSamplesProperties = bioSamplesProperties;
  }

  @GetMapping(value = "/")
  public String index(final Model model) {

    //    String jsonStats;
    //    String jsonYearlyGrowth;
    //    try {
    //      ObjectMapper mapper = new ObjectMapper();
    //      jsonStats = mapper.writeValueAsString(statService.getStats());
    //      jsonYearlyGrowth = mapper.writeValueAsString(statService.getBioSamplesYearlyGrowth());
    //    } catch (JsonProcessingException e) {
    //      jsonStats = "{}";
    //      jsonYearlyGrowth = "{}";
    //    }

    final JsonLDDataCatalog dataCatalog = jsonLDService.getBioSamplesDataCatalog();
    model.addAttribute("jsonLD", jsonLDService.jsonLDToString(dataCatalog));
    //    model.addAttribute("stats", jsonStats);
    //    model.addAttribute("yearlyGrowth", jsonYearlyGrowth);
    return "index";
  }

  @GetMapping(value = "/about")
  public String about() {
    return "about";
  }

  @GetMapping(value = "/submit")
  public String submit() {
    return "submit";
  }

  @GetMapping(value = "/samples")
  public String samples(
      final Model model,
      @RequestParam(name = "text", required = false) final String text,
      @RequestParam(name = "filter", required = false) final String[] filtersArray,
      @RequestParam(name = "page", defaultValue = "1") Integer page,
      @RequestParam(name = "size", defaultValue = "10") Integer size,
      final HttpServletRequest request,
      final HttpServletResponse response) {

    page = page == null || page < 1 ? 1 : page;
    size = size == null || size < 1 ? 10 : size;
    if (page > 500 || size > 200) {
      throw new PaginationException(); // solr degrades with high page and size params, use cursor
      // instead
    }

    final Collection<Filter> filterCollection = filterService.getFiltersCollection(filtersArray);
    final Collection<String> domains = bioSamplesAapService.getDomains();

    final Pageable pageable = PageRequest.of(page - 1, size);
    final Page<Sample> pageSample =
        samplePageService.getSamplesByText(
            text, filterCollection, domains, null, pageable, Optional.empty());

    // build URLs for the facets depending on if they are enabled or not
    final UriComponentsBuilder uriBuilder = ServletUriComponentsBuilder.fromRequest(request);
    final List<String> filtersList = new ArrayList<>();
    if (filtersArray != null) {
      filtersList.addAll(Arrays.asList(filtersArray));
    }
    Collections.sort(filtersList);
    final String downloadURL =
        "?text="
            + (text != null ? text : "")
            + "&filter="
            + StringUtils.join(filtersList, "&filter=");

    final JsonLDDataset jsonLDDataset = jsonLDService.getBioSamplesDataset();

    model.addAttribute("text", text);
    model.addAttribute("start", (page - 1) * size);
    model.addAttribute("page", pageSample);
    model.addAttribute("filters", filtersList);
    model.addAttribute("paginations", getPaginations(pageSample, uriBuilder));
    model.addAttribute("jsonLD", jsonLDService.jsonLDToString(jsonLDDataset));
    model.addAttribute("downloadURL", downloadURL);

    addCacheControlHeadersToResponse(
        domains, response, bioSamplesProperties.getBiosamplesCorePageCacheMaxAge());
    return "samples";
  }

  @GetMapping(value = "/graph/search")
  public String samplesGraph() {
    //		return "error/feature_not_supported"; //until this is ready for the production
    return "samples_graph";
  }

  @GetMapping(value = "/uploadLogin")
  public String login(final Model model) {
    final AuthorizationProvider[] loginWays = AuthorizationProvider.values();
    final List<String> logins = new ArrayList<>();

    for (final AuthorizationProvider loginWay : loginWays) {
      logins.add(loginWay.toString());
    }

    model.addAttribute("ways", logins);

    return "uploadLogin";
  }

  @GetMapping(value = "/facets")
  public String facets(
      final Model model,
      @RequestParam(name = "text", required = false) final String text,
      @RequestParam(name = "filter", required = false) final String[] filtersArray,
      final HttpServletResponse response) {
    final Collection<Filter> filterCollection = filterService.getFiltersCollection(filtersArray);
    final Collection<String> domains = bioSamplesAapService.getDomains();

    // build URLs for the facets depending on if they are enabled or not
    final List<String> filtersList = new ArrayList<>();
    if (filtersArray != null) {
      filtersList.addAll(Arrays.asList(filtersArray));
    }
    Collections.sort(filtersList);

    model.addAttribute("filters", filtersList);
    model.addAttribute("facets", facetService.getFacets(text, filterCollection, domains, 20, 10));

    addCacheControlHeadersToResponse(
        domains, response, bioSamplesProperties.getBiosamplesCoreFacetCacheMaxAge());
    return "fragments/facets";
  }

  private Paginations getPaginations(
      final Page<Sample> pageSample, final UriComponentsBuilder uriBuilder) {

    final int pageTotal = pageSample.getTotalPages();
    final int pageCurrent = pageSample.getNumber() + 1;

    Pagination previous = null;
    if (pageCurrent > 1) {
      previous = new Pagination(pageCurrent - 1, false, pageCurrent, uriBuilder);
    }

    Pagination next = null;
    if (pageCurrent < pageTotal) {
      next = new Pagination(pageCurrent + 1, false, pageCurrent, uriBuilder);
    }

    final Paginations paginations = new Paginations(pageCurrent, pageTotal, previous, next);

    if (pageTotal <= 6) {
      // few enough pages to fit onto a single bar
      for (int i = 1; i <= pageTotal; i++) {
        paginations.add(new Pagination(i, false, pageCurrent, uriBuilder));
      }
    } else {
      // need at least one ellipsis
      // if we are in the first 4 or the last 4
      if (pageCurrent <= 4) {
        paginations.add(new Pagination(1, false, pageCurrent, uriBuilder));
        paginations.add(new Pagination(2, false, pageCurrent, uriBuilder));
        paginations.add(new Pagination(3, false, pageCurrent, uriBuilder));
        paginations.add(new Pagination(4, false, pageCurrent, uriBuilder));
        paginations.add(new Pagination(5, false, pageCurrent, uriBuilder));
        paginations.add(new Pagination(pageTotal, true, pageCurrent, uriBuilder));
      } else if (pageTotal - pageCurrent <= 3) {
        paginations.add(new Pagination(1, false, pageCurrent, uriBuilder));
        paginations.add(new Pagination(pageTotal - 4, true, pageCurrent, uriBuilder));
        paginations.add(new Pagination(pageTotal - 3, false, pageCurrent, uriBuilder));
        paginations.add(new Pagination(pageTotal - 2, false, pageCurrent, uriBuilder));
        paginations.add(new Pagination(pageTotal - 1, false, pageCurrent, uriBuilder));
        paginations.add(new Pagination(pageTotal, false, pageCurrent, uriBuilder));
      } else {
        // will need two sets of ellipsis
        paginations.add(new Pagination(1, false, pageCurrent, uriBuilder));
        paginations.add(new Pagination(pageCurrent - 1, true, pageCurrent, uriBuilder));
        paginations.add(new Pagination(pageCurrent, false, pageCurrent, uriBuilder));
        paginations.add(new Pagination(pageCurrent + 1, false, pageCurrent, uriBuilder));
        paginations.add(new Pagination(pageTotal, true, pageCurrent, uriBuilder));
      }
    }

    return paginations;
  }

  private static class Paginations implements Iterable<Pagination> {

    private final List<Pagination> paginations = new ArrayList<>();
    public final Pagination previous;
    public final Pagination next;
    public final int current;
    public final int total;

    Paginations(
        final int current, final int total, final Pagination previous, final Pagination next) {
      this.current = current;
      this.total = total;
      this.previous = previous;
      this.next = next;
    }

    public void add(final Pagination pagination) {
      paginations.add(pagination);
    }

    @Override
    public Iterator<Pagination> iterator() {
      return paginations.iterator();
    }
  }

  private static class Pagination {
    public final int page;
    public final String url;
    public final boolean skip;
    public final boolean current;

    Pagination(
        final int pageNo,
        final boolean skip,
        final int currentNo,
        final UriComponentsBuilder uriBuilder) {
      page = pageNo;
      this.skip = skip;
      current = (currentNo == pageNo);
      url = uriBuilder.cloneBuilder().replaceQueryParam("page", pageNo).build().toUriString();
    }
  }

  @GetMapping(value = "/samples/{accession}")
  public String samplesAccession(
      final Model model,
      @PathVariable final String accession,
      final HttpServletRequest request,
      final HttpServletResponse response) {
    // TODO allow curation domain specification
    final Optional<Sample> sample = sampleService.fetch(accession, Optional.empty());
    if (!sample.isPresent()) {
      log.info("Returning a 404 for " + request.getRequestURL());
      throw new ResourceNotFoundException();
    }

    bioSamplesAapService.isSampleAccessible(sample.get());

    // response.setHeader(HttpHeaders.LAST_MODIFIED,
    // String.valueOf(sample.getUpdate().toEpochSecond(ZoneOffset.UTC)));
    // response.setHeader(HttpHeaders.ETAG, String.valueOf(sample.hashCode()));

    final String jsonLDString =
        jsonLDService.jsonLDToString(jsonLDService.sampleToJsonLD(sample.get()));
    model.addAttribute("sample", sample.get());
    model.addAttribute("schemaStoreUrl", bioSamplesProperties.getSchemaStore());
    model.addAttribute("jsonLD", jsonLDString);

    model.addAttribute("update", sample.get().getUpdate().atOffset(ZoneOffset.UTC));
    model.addAttribute("release", sample.get().getRelease().atOffset(ZoneOffset.UTC));
    model.addAttribute("create", sample.get().getCreate().atOffset(ZoneOffset.UTC));

    final Instant submitted = sample.get().getSubmitted();
    model.addAttribute("submitted", submitted != null ? submitted.atOffset(ZoneOffset.UTC) : null);
    final Instant reviewed = sample.get().getReviewed();
    model.addAttribute("reviewed", reviewed != null ? reviewed.atOffset(ZoneOffset.UTC) : null);

    addCacheControlHeadersToResponse(new ArrayList<>(), response, 10);
    return "sample";
  }

  @GetMapping("/sample/{accession}")
  public String sampleAccession(@PathVariable final String accession) {
    return "redirect:/samples/" + accession;
  }

  @GetMapping("/sample")
  public String sample() {
    return "redirect:/samples";
  }

  @GetMapping("/group/{accession}")
  public String groupAccession(@PathVariable final String accession) {
    return "redirect:/samples/" + accession;
  }

  @GetMapping("/group")
  public String group() {
    return "redirect:/samples";
  }

  @GetMapping("/groups/{accession}")
  public String groupsAccession(@PathVariable final String accession) {
    return "redirect:/samples/" + accession;
  }

  @GetMapping("/groups")
  public String groups() {
    return "redirect:/samples";
  }

  private void addCacheControlHeadersToResponse(
      final Collection<String> domains, final HttpServletResponse response, final long maxAge) {
    // EBI load balancer does cache but doesn't add age header, so clients could cache up to twice
    // this age
    final CacheControl cacheControl = CacheControl.maxAge(maxAge, TimeUnit.SECONDS);
    // if the user has access to any domains, then mark the response as
    // private as must be using AAP and responses will be different
    if (domains != null && !domains.isEmpty()) {
      cacheControl.cachePrivate();
    }
    response.setHeader("Cache-Control", cacheControl.getHeaderValue());
  }
}
