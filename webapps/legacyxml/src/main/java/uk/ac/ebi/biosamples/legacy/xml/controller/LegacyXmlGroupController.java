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
package uk.ac.ebi.biosamples.legacy.xml.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.exception.SampleNotFoundException;
import uk.ac.ebi.biosamples.legacy.xml.service.LegacyQueryParser;
import uk.ac.ebi.biosamples.legacy.xml.service.SummaryInfoService;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.legacyxml.BioSample;
import uk.ac.ebi.biosamples.model.legacyxml.BioSampleGroup;
import uk.ac.ebi.biosamples.model.legacyxml.ResultQuery;
import uk.ac.ebi.biosamples.service.FilterBuilder;

@RestController
public class LegacyXmlGroupController {

  private Logger log = LoggerFactory.getLogger(getClass());

  private final BioSamplesClient client;
  private final SummaryInfoService summaryInfoService;
  private final LegacyQueryParser legacyQueryParser;

  private final Filter groupAccessionFilter =
      FilterBuilder.create().onAccession("SAMEG[0-9]+").build();

  public LegacyXmlGroupController(
      BioSamplesClient client,
      SummaryInfoService summaryInfoService,
      LegacyQueryParser legacyQueryParser) {
    this.client = client;
    this.summaryInfoService = summaryInfoService;
    this.legacyQueryParser = legacyQueryParser;
  }

  @CrossOrigin
  @GetMapping(
      value = "/v1.0/groups/{accession:SAMEG\\d+}",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
  public Sample getGroupv1p0(@PathVariable String accession) throws IOException {
    log.warn(
        "ACCESSING DEPRECATED API at LegacyXmlGroupController /v1.0/groups/{accession:SAMEG\\d+}");
    Optional<Resource<Sample>> sample = client.fetchSampleResource(accession);

    if (sample.isPresent()) {
      log.trace("Found sample " + accession + " as " + sample.get());
      return sample.get().getContent();
    } else {
      log.trace("Did not find sample " + accession);
      throw new SampleNotFoundException();
    }
  }

  @CrossOrigin
  @GetMapping(
      value = "/groups/{accession:SAMEG\\d+}",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
  public Sample getGroup(@PathVariable String accession) throws IOException {
    log.warn("ACCESSING DEPRECATED API at LegacyXmlGroupController /");
    return getGroupv1p0(accession);
  }

  @CrossOrigin
  @GetMapping(
      value = {"/v1.0/groups"},
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
  public ResultQuery getv1p0Groups(
      @RequestParam(name = "query", defaultValue = "*") String query,
      @RequestParam(name = "pagesize", defaultValue = "25") int pagesize,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "sort", defaultValue = "desc") String sort) {
    log.warn("ACCESSING DEPRECATED API at LegacyXmlGroupController /v1.0/groups");
    if (page < 1) {
      throw new IllegalArgumentException("Page parameter has to be 1-based");
    }

    List<Filter> filterList = new ArrayList<>();
    filterList.add(groupAccessionFilter);

    if (legacyQueryParser.queryContainsDateRangeFilter(query)) {

      Optional<Filter> dateRangeFilters = legacyQueryParser.extractDateFilterFromQuery(query);
      dateRangeFilters.ifPresent(filterList::add);
    }

    if (legacyQueryParser.queryContainsSampleFilter(query)) {
      Optional<Filter> accessionFilter = legacyQueryParser.extractAccessionFilterFromQuery(query);
      accessionFilter.ifPresent(filterList::add);
    }

    query = legacyQueryParser.cleanQueryFromKnownFilters(query);

    PagedResources<Resource<Sample>> results =
        client.fetchPagedSampleResource(query, filterList, page - 1, pagesize);

    ResultQuery resultQuery = new ResultQuery();

    resultQuery.setSummaryInfo(summaryInfoService.fromPagedGroupResources(results));

    for (Resource<Sample> resource : results.getContent()) {
      BioSampleGroup bioSampleGroup = new BioSampleGroup();
      bioSampleGroup.setId(resource.getContent().getAccession());
      resultQuery.getBioSampleGroup().add(bioSampleGroup);
    }

    return resultQuery;
  }

  @CrossOrigin
  @GetMapping(
      value = {"/groups"},
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
  public ResultQuery getGroups(
      @RequestParam(name = "query", defaultValue = "*") String query,
      @RequestParam(name = "pagesize", defaultValue = "25") int pagesize,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "sort", defaultValue = "desc") String sort) {
    log.warn("ACCESSING DEPRECATED API at LegacyXmlGroupController /groups");
    return getv1p0Groups(query, pagesize, page, sort);
  }

  @CrossOrigin
  @GetMapping(
      value = {"/v1.0/groupsamples/{groupAccession:SAMEG\\d+}"},
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
  public @ResponseBody ResultQuery getv1p0SamplesInGroup(
      @PathVariable String groupAccession,
      @RequestParam(name = "query", defaultValue = "*") String query,
      @RequestParam(name = "pagesize", defaultValue = "25") int pagesize,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "sort", defaultValue = "desc") String sort) {
    log.warn(
        "ACCESSING DEPRECATED API at LegacyXmlGroupController /v1.0/groupsamples/{groupAccession:SAMEG\\d+}");

    //        Sort.Direction sort =
    // Sort.Direction.fromString(queryParams.getOrDefault("sort","desc"));
    List<Filter> filterList = new ArrayList<>();
    filterList.add(
        FilterBuilder.create().onInverseRelation("has member").withValue(groupAccession).build());

    if (legacyQueryParser.queryContainsDateRangeFilter(query)) {

      Optional<Filter> dateRangeFilters = legacyQueryParser.extractDateFilterFromQuery(query);
      dateRangeFilters.ifPresent(filterList::add);
    }

    if (legacyQueryParser.queryContainsSampleFilter(query)) {
      Optional<Filter> accessionFilter = legacyQueryParser.extractAccessionFilterFromQuery(query);
      accessionFilter.ifPresent(filterList::add);
    }

    query = legacyQueryParser.cleanQueryFromKnownFilters(query);

    PagedResources<Resource<Sample>> results =
        client.fetchPagedSampleResource(query, filterList, page - 1, pagesize);

    ResultQuery resultQuery = new ResultQuery();

    resultQuery.setSummaryInfo(summaryInfoService.fromPagedGroupResources(results));

    for (Resource<Sample> resource : results.getContent()) {
      BioSample biosample = new BioSample();
      biosample.setId(resource.getContent().getAccession());
      resultQuery.getBioSample().add(biosample);
    }

    return resultQuery;
  }

  @CrossOrigin
  @GetMapping(
      value = {"/groupsamples/{groupAccession:SAMEG\\d+}"},
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
  public @ResponseBody ResultQuery getSamplesInGroup(
      @PathVariable String groupAccession,
      @RequestParam(name = "query", defaultValue = "*") String query,
      @RequestParam(name = "pagesize", defaultValue = "25") int pagesize,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "sort", defaultValue = "desc") String sort) {
    log.warn(
        "ACCESSING DEPRECATED API at LegacyXmlGroupController /groupsamples/{groupAccession:SAMEG\\d+}");
    return getv1p0SamplesInGroup(groupAccession, query, pagesize, page, sort);
  }
}
