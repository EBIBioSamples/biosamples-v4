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
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.JsonLDDataCatalog;
import uk.ac.ebi.biosamples.model.JsonLDDataset;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.*;

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

  private Logger log = LoggerFactory.getLogger(getClass());

  private final SampleService sampleService;
  private final SamplePageService samplePageService;
  private final JsonLDService jsonLDService;
  private final FacetService facetService;
  private final FilterService filterService;
  private final BioSamplesAapService bioSamplesAapService;
  private final BioSamplesProperties bioSamplesProperties;

  public SampleHtmlController(
      SampleService sampleService,
      SamplePageService samplePageService,
      JsonLDService jsonLDService,
      FacetService facetService,
      FilterService filterService,
      BioSamplesAapService bioSamplesAapService,
      BioSamplesProperties bioSamplesProperties) {
    this.sampleService = sampleService;
    this.samplePageService = samplePageService;
    this.jsonLDService = jsonLDService;
    this.facetService = facetService;
    this.filterService = filterService;
    this.bioSamplesAapService = bioSamplesAapService;
    this.bioSamplesProperties = bioSamplesProperties;
  }

  @GetMapping(value = "/")
  public String index(Model model) {

    JsonLDDataCatalog dataCatalog = jsonLDService.getBioSamplesDataCatalog();
    model.addAttribute("jsonLD", jsonLDService.jsonLDToString(dataCatalog));
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

  // TODO: 2018/10/29 Maintaining old method for legacy purpose, we can think of deleting this if
  // no-one is actually using it
  //	@GetMapping(value = "/samples")
  //	public String oldSamples(Model model, @RequestParam(name="text", required=false) String text,
  //			@RequestParam(name="filter", required=false) String[] filtersArray,
  //			@RequestParam(name="start", defaultValue="0") Integer start,
  //			@RequestParam(name="rows", defaultValue="10") Integer rows,
  //			HttpServletRequest request, HttpServletResponse response) {
  //		return this.samples(model, text, filtersArray, start/rows, rows, request, response);
  //	}

  @GetMapping(value = "/samples")
  public String samples(
      Model model,
      @RequestParam(name = "text", required = false) String text,
      @RequestParam(name = "filter", required = false) String[] filtersArray,
      @RequestParam(name = "page", defaultValue = "1") Integer page,
      @RequestParam(name = "size", defaultValue = "10") Integer size,
      @RequestParam(name = "curationrepo", defaultValue = "none") final String curationRepo,
      HttpServletRequest request,
      HttpServletResponse response) {

    // force a minimum of 1 result
    if (size < 1) {
      size = 1;
    }
    // cap it for our protection
    if (size > 1000) {
      size = 1000;
    }

    if (page < 1) {
      page = 1;
    }

    Collection<Filter> filterCollection = filterService.getFiltersCollection(filtersArray);
    Collection<String> domains = bioSamplesAapService.getDomains();

    Pageable pageable = new PageRequest(page - 1, size);
    Page<Sample> pageSample =
        samplePageService.getSamplesByText(text, filterCollection, domains, pageable, curationRepo);

    // default to getting 10 values from 10 facets
    // List<Facet> sampleFacets = facetService.getFacets(text, filterCollection, domains, 10,
    // 10);

    // build URLs for the facets depending on if they are enabled or not
    UriComponentsBuilder uriBuilder = ServletUriComponentsBuilder.fromRequest(request);
    List<String> filtersList = new ArrayList<>();
    if (filtersArray != null) {
      filtersList.addAll(Arrays.asList(filtersArray));
    }
    Collections.sort(filtersList);

    JsonLDDataset jsonLDDataset = jsonLDService.getBioSamplesDataset();

    model.addAttribute("text", text);
    model.addAttribute("start", (page - 1) * size);
    model.addAttribute("page", pageSample);
    model.addAttribute("filters", filtersList);
    model.addAttribute("paginations", getPaginations(pageSample, uriBuilder));
    model.addAttribute("jsonLD", jsonLDService.jsonLDToString(jsonLDDataset));
    //    model.addAttribute(
    //        "facets",
    //        new LazyContextVariable<List<Facet>>() {
    //          @Override
    //          protected List<Facet> loadValue() {
    //            return facetService.getFacets(text, filterCollection, domains, 10, 10);
    //          }
    //        });

    // TODO add "clear all facets" button
    // TODO title of webpage

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
    response.setHeader("Cache-Control", cacheControl.getHeaderValue());
    return "samples";
  }

  @GetMapping(value = "/graph/search")
  public String samplesGraph(
      Model model, HttpServletRequest request, HttpServletResponse response) {
    //		return "error/feature_not_supported"; //until this is ready for the production
    return "samples_graph";
  }

  @GetMapping(value = "/upload")
  public String upload(Model model, HttpServletRequest request, HttpServletResponse response) {
    return "upload";
  }

  @GetMapping(value = "/facets")
  public String facets(
      Model model,
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
    // default to getting 10 values from 10 facets
    model.addAttribute("facets", facetService.getFacets(text, filterCollection, domains, 20, 10));

    // Note - EBI load balancer does cache but doesn't add age header, so clients could cache up
    // to
    // twice this age
    CacheControl cacheControl =
        CacheControl.maxAge(
            bioSamplesProperties.getBiosamplesCoreFacetCacheMaxAge(), TimeUnit.SECONDS);
    // if the user has access to any domains, then mark the response as private as must be using
    // AAP
    // and responses will be different
    if (!domains.isEmpty()) {
      cacheControl.cachePrivate();
    }

    response.setHeader("Cache-Control", cacheControl.getHeaderValue());
    return "fragments/facets";
  }

  private Paginations getPaginations(Page<Sample> pageSample, UriComponentsBuilder uriBuilder) {

    int pageTotal = pageSample.getTotalPages();
    int pageCurrent = pageSample.getNumber() + 1;

    Pagination previous = null;
    if (pageCurrent > 1) {
      previous = new Pagination(pageCurrent - 1, false, pageCurrent, uriBuilder, pageSample);
    }

    Pagination next = null;
    if (pageCurrent < pageTotal) {
      next = new Pagination(pageCurrent + 1, false, pageCurrent, uriBuilder, pageSample);
    }

    Paginations paginations = new Paginations(pageCurrent, pageTotal, previous, next);

    if (pageTotal <= 6) {
      // few enough pages to fit onto a single bar
      for (int i = 1; i <= pageTotal; i++) {
        paginations.add(new Pagination(i, false, pageCurrent, uriBuilder, pageSample));
      }
    } else {
      // need at least one ellipsis
      // if we are in the first 4 or the last 4
      if (pageCurrent <= 4) {
        paginations.add(new Pagination(1, false, pageCurrent, uriBuilder, pageSample));
        paginations.add(new Pagination(2, false, pageCurrent, uriBuilder, pageSample));
        paginations.add(new Pagination(3, false, pageCurrent, uriBuilder, pageSample));
        paginations.add(new Pagination(4, false, pageCurrent, uriBuilder, pageSample));
        paginations.add(new Pagination(5, false, pageCurrent, uriBuilder, pageSample));
        paginations.add(new Pagination(pageTotal, true, pageCurrent, uriBuilder, pageSample));
      } else if (pageTotal - pageCurrent <= 3) {
        paginations.add(new Pagination(1, false, pageCurrent, uriBuilder, pageSample));
        paginations.add(new Pagination(pageTotal - 4, true, pageCurrent, uriBuilder, pageSample));
        paginations.add(new Pagination(pageTotal - 3, false, pageCurrent, uriBuilder, pageSample));
        paginations.add(new Pagination(pageTotal - 2, false, pageCurrent, uriBuilder, pageSample));
        paginations.add(new Pagination(pageTotal - 1, false, pageCurrent, uriBuilder, pageSample));
        paginations.add(new Pagination(pageTotal, false, pageCurrent, uriBuilder, pageSample));
      } else {
        // will need two sets of ellipsis
        paginations.add(new Pagination(1, false, pageCurrent, uriBuilder, pageSample));
        paginations.add(new Pagination(pageCurrent - 1, true, pageCurrent, uriBuilder, pageSample));
        paginations.add(new Pagination(pageCurrent, false, pageCurrent, uriBuilder, pageSample));
        paginations.add(
            new Pagination(pageCurrent + 1, false, pageCurrent, uriBuilder, pageSample));
        paginations.add(new Pagination(pageTotal, true, pageCurrent, uriBuilder, pageSample));
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

    public Paginations(int current, int total, Pagination previous, Pagination next) {
      this.current = current;
      this.total = total;
      this.previous = previous;
      this.next = next;
    }

    public void add(Pagination pagination) {
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

    public Pagination(
        int pageNo,
        boolean skip,
        int currentNo,
        UriComponentsBuilder uriBuilder,
        Page<Sample> pageSample) {
      this.page = pageNo;
      this.skip = skip;
      this.current = (currentNo == pageNo);
      this.url = uriBuilder.cloneBuilder().replaceQueryParam("page", pageNo).build().toUriString();
    }
  }

  private URI getFilterUri(
      UriComponentsBuilder uriBuilder,
      List<String> filters,
      String filterAdd,
      String filterRemove) {
    List<String> tempFiltersList = new ArrayList<>(filters);
    if (filterAdd != null) {
      tempFiltersList.add(filterAdd);
      // if turning on a facet-all filter, remove facet-value filters for that facet
      // if turning on a facet-value filter, remove facet-all filters for that facet
      if (filterAdd.contains(":")) {
        // remove facet-all filters when adding a specific facet
        tempFiltersList.remove(filterAdd.split(":")[0]);
      } else {
        // remove facet-specific filters when adding a filter-all facet
        Iterator<String> it = tempFiltersList.iterator();
        while (it.hasNext()) {
          if (it.next().startsWith(filterAdd + ":")) {
            it.remove();
          }
        }
      }
    }
    if (filterRemove != null) {
      tempFiltersList.remove(filterRemove);
    }
    Collections.sort(tempFiltersList);
    String[] tempFiltersArray = new String[tempFiltersList.size()];
    tempFiltersArray = tempFiltersList.toArray(tempFiltersArray);
    URI uri =
        uriBuilder
            .cloneBuilder()
            .replaceQueryParam("filter", (Object[]) tempFiltersArray)
            .replaceQueryParam("start") // reset back to page 1
            .build(false)
            .toUri();
    return uri;
  }

  @GetMapping(value = "/samples/{accession}")
  public String samplesAccession(
      Model model,
      @PathVariable String accession,
      @RequestParam(name = "curationrepo", required = false) final String curationRepo,
      HttpServletRequest request,
      HttpServletResponse response) {
    // TODO allow curation domain specification
    Optional<Sample> sample = sampleService.fetch(accession, Optional.empty(), curationRepo);
    if (!sample.isPresent()) {
      // did not exist, throw 404
      // TODO do as an exception
      log.info("Returning a 404 for " + request.getRequestURL());
      //			response.setStatus(HttpStatus.NOT_FOUND.value());
      //			return "error/4xx";
      throw new ResourceNotFoundException();
    }

    if (sample == null || !sample.isPresent()) {
      // throw internal server error
      throw new RuntimeException("Unable to retrieve " + accession);
    }

    bioSamplesAapService.checkAccessible(sample.get());

    // response.setHeader(HttpHeaders.LAST_MODIFIED,
    // String.valueOf(sample.getUpdate().toEpochSecond(ZoneOffset.UTC)));
    // response.setHeader(HttpHeaders.ETAG, String.valueOf(sample.hashCode()));

    String jsonLDString = jsonLDService.jsonLDToString(jsonLDService.sampleToJsonLD(sample.get()));
    model.addAttribute("sample", sample.get());
    model.addAttribute("jsonLD", jsonLDString);
    // becuase thymleaf can only work with timezoned temporals, not instant
    // we need to do the conversion
    model.addAttribute("update", sample.get().getUpdate().atOffset(ZoneOffset.UTC));
    model.addAttribute("release", sample.get().getRelease().atOffset(ZoneOffset.UTC));
    model.addAttribute("create", sample.get().getCreate().atOffset(ZoneOffset.UTC));

    Instant submitted = sample.get().getSubmitted();

    if (submitted != null) model.addAttribute("submitted", submitted.atOffset(ZoneOffset.UTC));
    else model.addAttribute("submitted", null);

    Instant reviewed = sample.get().getReviewed();

    if (reviewed != null) model.addAttribute("reviewed", reviewed.atOffset(ZoneOffset.UTC));
    else model.addAttribute("reviewed", null);

    return "sample";
  }

  @GetMapping("/sample/{accession}")
  public String sampleAccession(@PathVariable String accession) {
    return "redirect:/samples/" + accession;
  }

  @GetMapping("/sample")
  public String sample() {
    return "redirect:/samples";
  }

  @GetMapping("/group/{accession}")
  public String groupAccession(@PathVariable String accession) {
    return "redirect:/samples/" + accession;
  }

  @GetMapping("/group")
  public String group() {
    return "redirect:/samples";
  }

  @GetMapping("/groups/{accession}")
  public String groupsAccession(@PathVariable String accession) {
    return "redirect:/samples/" + accession;
  }

  @GetMapping("/groups")
  public String groups() {
    return "redirect:/samples";
  }
}
