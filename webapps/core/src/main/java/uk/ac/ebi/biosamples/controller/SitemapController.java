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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.expression.ParseException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.SamplePageService;

@Controller
@RequestMapping("/sitemap")
public class SitemapController {
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Value("${model.page.size:10}")
  private int sitemapPageSize;

  private final SamplePageService samplePageService;

  public SitemapController(final SamplePageService pageService) {
    samplePageService = pageService;
  }

  /**
   * Generate the sitemap index
   *
   * @param request the request
   * @return the sitemap index in xml format
   */
  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_XML_VALUE)
  @ResponseBody
  public XmlSitemapIndex createSampleSitemapIndex(final HttpServletRequest request) {

    final long sampleCount = getTotalSamples();
    final long pageNumber = (sampleCount / (long) sitemapPageSize) + 1L;
    final XmlSitemapIndex xmlSitemapIndex = new XmlSitemapIndex();
    for (int i = 0; i < pageNumber; i++) {
      final String location = generateBaseUrl(request) + String.format("/sitemap/%d", i + 1);
      final XmlSitemap xmlSiteMap = new XmlSitemap(location);
      xmlSitemapIndex.addSitemap(xmlSiteMap);
    }
    return xmlSitemapIndex;
  }

  /**
   * Generate a sitemap subpage
   *
   * @param pageNumber the page number
   * @param request the request object
   * @return the sitemap page content
   * @throws ParseException
   */
  @RequestMapping(
      value = "/{id}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_XML_VALUE)
  @ResponseBody
  public XmlUrlSet createSampleSitemapPage(
      @PathVariable("id") final int pageNumber, final HttpServletRequest request)
      throws ParseException {
    final long startTime = System.currentTimeMillis();
    final Pageable pageRequest = PageRequest.of(pageNumber - 1, sitemapPageSize);
    final Page<Sample> samplePage =
        samplePageService.getSamplesByText(
            "", Collections.emptyList(), null, pageRequest, Optional.empty());
    final XmlUrlSet xmlUrlSet = new XmlUrlSet();
    for (final Sample sample : samplePage.getContent()) {
      final String location =
          generateBaseUrl(request) + String.format("/samples/%s", sample.getAccession());

      final LocalDate lastModifiedDate =
          LocalDateTime.ofInstant(sample.getUpdate(), ZoneOffset.UTC).toLocalDate();

      final XmlUrl url =
          new XmlUrl.XmlUrlBuilder(location)
              .lastModified(lastModifiedDate)
              .hasPriority(XmlUrl.Priority.MEDIUM)
              .build();
      xmlUrlSet.addUrl(url);
    }
    log.debug(
        String.format(
            "Returning model for %d samples took %d millis",
            sitemapPageSize, System.currentTimeMillis() - startTime));
    return xmlUrlSet;
  }

  /**
   * Generate the proper url based on the request
   *
   * @param request
   * @return the base url for the links in the sitemap
   */
  private String generateBaseUrl(final HttpServletRequest request) {
    final String requestURI = request.getRequestURI();
    final String requestURL = request.getRequestURL().toString();
    return requestURL.replaceFirst(requestURI, "") + request.getContextPath();
  }

  /**
   * Get the total number of samples in the database
   *
   * @return the number of samples
   */
  private long getTotalSamples() {
    final Pageable pageable = PageRequest.of(0, 1);
    final Collection<Filter> filters = Collections.emptyList();
    final Collection<String> domains = Collections.emptyList();
    final Page<Sample> samplePage =
        samplePageService.getSamplesByText("", filters, null, pageable, Optional.empty());
    return samplePage.getTotalElements();
  }
}
