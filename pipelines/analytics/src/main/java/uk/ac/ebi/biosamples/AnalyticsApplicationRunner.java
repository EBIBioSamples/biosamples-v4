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
package uk.ac.ebi.biosamples;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SampleAnalytics;
import uk.ac.ebi.biosamples.model.facet.Facet;
import uk.ac.ebi.biosamples.model.facet.content.LabelCountEntry;
import uk.ac.ebi.biosamples.model.facet.content.LabelCountListContent;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationRuleRepository;
import uk.ac.ebi.biosamples.service.FacetService;
import uk.ac.ebi.biosamples.service.SamplePageService;
import uk.ac.ebi.biosamples.utils.MailSender;
import uk.ac.ebi.biosamples.utils.mongo.AnalyticsService;

@Component
public class AnalyticsApplicationRunner implements ApplicationRunner {
  private static final Logger LOG = LoggerFactory.getLogger(AnalyticsApplicationRunner.class);

  private final BioSamplesClient bioSamplesClient;
  private final PipelinesProperties pipelinesProperties;
  private final Map<String, String> curationRules;
  private final MongoCurationRuleRepository repository;
  private final AnalyticsService analyticsService;
  private final PipelineFutureCallback pipelineFutureCallback;
  private final FacetService facetService;
  private final SamplePageService samplePageService;

  public AnalyticsApplicationRunner(
      BioSamplesClient bioSamplesClient,
      PipelinesProperties pipelinesProperties,
      MongoCurationRuleRepository repository,
      AnalyticsService analyticsService,
      FacetService facetService,
      SamplePageService samplePageService) {
    this.bioSamplesClient = bioSamplesClient;
    this.pipelinesProperties = pipelinesProperties;
    this.repository = repository;
    this.analyticsService = analyticsService;
    this.facetService = facetService;
    this.samplePageService = samplePageService;
    this.curationRules = new HashMap<>();
    this.pipelineFutureCallback = new PipelineFutureCallback();
  }

  @Override
  public void run(ApplicationArguments args) {
    Instant startTime = Instant.now();
    LOG.info("Pipeline started at {}", startTime);
    long sampleCount = 0;
    boolean isPassed = true;
    SampleAnalytics sampleAnalytics = new SampleAnalytics();

    Page<Sample> samplePage =
        samplePageService.getSamplesByText(
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            null,
            PageRequest.of(0, 1),
            null,
            Optional.empty());
    sampleAnalytics.setTotalRecords(samplePage.getTotalElements());
    addToFacets("organism", sampleAnalytics);
    addToFacets("tissue", sampleAnalytics);
    addToFacets("sex", sampleAnalytics);
    addToFacets("external reference", sampleAnalytics);

    Instant endTime = Instant.now();
    LOG.info("Total samples processed {}", sampleCount);
    LOG.info("Total curation objects added {}", pipelineFutureCallback.getTotalCount());
    LOG.info("Pipeline finished at {}", endTime);
    LOG.info(
        "Pipeline total running time {} seconds",
        Duration.between(startTime, endTime).getSeconds());

    analyticsService.mergeSampleAnalytics(startTime, sampleAnalytics);
    MailSender.sendEmail("Analytics", null, isPassed);
  }

  private void addToFacets(String facetField, SampleAnalytics sampleAnalytics) {
    List<Facet> facetList =
        facetService.getFacets(
            "", Collections.emptyList(), Collections.emptyList(), 1, 10, facetField);
    for (Facet facet : facetList) {
      String label = facet.getLabel();
      Long totalCount = facet.getCount();
      Long existingFacetSum = 0L;
      Map<String, Map<String, Long>> facetListMap = sampleAnalytics.getFacets();
      if (facet.getContent() instanceof LabelCountListContent) {
        Map<String, Long> facetMap = new HashMap<>();
        facetListMap.put(label, facetMap);
        for (LabelCountEntry e : (LabelCountListContent) facet.getContent()) {
          facetMap.put(e.getLabel(), e.getCount());
          existingFacetSum += e.getCount();
        }
        facetMap.put("other", totalCount - existingFacetSum);
      }
    }
  }
}
