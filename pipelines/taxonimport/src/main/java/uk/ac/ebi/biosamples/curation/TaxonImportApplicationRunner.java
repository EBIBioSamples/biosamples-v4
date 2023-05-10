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
package uk.ac.ebi.biosamples.curation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelineFutureCallback;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.PipelineAnalytics;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SampleAnalytics;
import uk.ac.ebi.biosamples.utils.mongo.AnalyticsService;

@Component
public class TaxonImportApplicationRunner implements ApplicationRunner {
  private static final Logger LOG = LoggerFactory.getLogger(TaxonImportApplicationRunner.class);

  private final BioSamplesClient bioSamplesClient;
  private final AnalyticsService analyticsService;
  private final PipelineFutureCallback pipelineFutureCallback;
  private final ObjectMapper objectMapper;

  public TaxonImportApplicationRunner(
      final BioSamplesClient bioSamplesClient,
      final AnalyticsService analyticsService,
      ObjectMapper objectMapper) {
    this.bioSamplesClient = bioSamplesClient;
    this.analyticsService = analyticsService;
    this.objectMapper = objectMapper;
    pipelineFutureCallback = new PipelineFutureCallback();
  }

  @Override
  public void run(final ApplicationArguments args) throws Exception {
    final Instant startTime = Instant.now();
    LOG.info("Pipeline started at {}", startTime);
    long curatedSampleCount = 0;
    final SampleAnalytics sampleAnalytics = new SampleAnalytics();

    List<TaxonEntry> taxonEntries = loadTaxonFile(getFileNameFromArgs(args));
    for (TaxonEntry entry : taxonEntries) {

      Optional<EntityModel<Sample>> optionalSample =
          bioSamplesClient.fetchSampleResource(
              entry.getBioSampleAccession(), Optional.of(Collections.singletonList("")));
      if (optionalSample.isPresent()) {
        Sample sample = optionalSample.get().getContent();
        Long oldTaxId = sample.getTaxId();
        Optional<Attribute> oldOrganism =
            sample.getCharacteristics().stream()
                .filter(c -> c.getType().equalsIgnoreCase("organism"))
                .findFirst();

        Attribute newOrganism = Attribute.build("organism", entry.getNcbiTaxonName());

        Sample newSample;
        Set<Attribute> sampleAttributes = sample.getAttributes();
        if (oldOrganism.isPresent()) {
          sampleAttributes.remove(oldOrganism.get());
          LOG.info(
              "Curating sample "
                  + entry.getBioSampleAccession()
                  + " : taxId = "
                  + oldTaxId
                  + " -> "
                  + entry.getTaxId()
                  + " : organism = "
                  + oldOrganism.get().getValue()
                  + " -> "
                  + entry.getBioSampleTaxName());
        } else {
          LOG.info(
              "Curating sample "
                  + entry.getBioSampleAccession()
                  + " : taxId = "
                  + oldTaxId
                  + " : organism = "
                  + entry.getBioSampleTaxName());
        }
        sampleAttributes.add(newOrganism);
        newSample = Sample.Builder.fromSample(sample).withTaxId(entry.getTaxId()).build();
        bioSamplesClient.persistSampleResource(newSample);
        curatedSampleCount++;

      } else {
        LOG.error("Failed to retrieve sample with the accession: " + entry.getBioSampleAccession());
      }
    }

    final Instant endTime = Instant.now();
    LOG.info("Total samples processed {}", taxonEntries.size());
    LOG.info("Total modified samples {}", curatedSampleCount);
    LOG.info("Pipeline finished at {}", endTime);
    LOG.info(
        "Pipeline total running time {} seconds",
        Duration.between(startTime, endTime).getSeconds());

    final PipelineAnalytics pipelineAnalytics =
        new PipelineAnalytics(
            "taxonimport",
            startTime,
            endTime,
            taxonEntries.size(),
            pipelineFutureCallback.getTotalCount());
    sampleAnalytics.setProcessedRecords(taxonEntries.size());
    analyticsService.persistSampleAnalytics(startTime, sampleAnalytics);
    analyticsService.persistPipelineAnalytics(pipelineAnalytics);
  }

  private List<TaxonEntry> loadTaxonFile(final String filePath) {
    List<TaxonEntry> taxonEntries;
    try {
      File taxonFile = new File(filePath);
      taxonEntries = objectMapper.readValue(taxonFile, new TypeReference<List<TaxonEntry>>() {});
    } catch (IOException e) {
      LOG.error("Failed to process json file", e);
      throw new RuntimeException(
          "Failed to process the json file. Pipeline failed to run. Exiting the pipeline.");
    }

    return taxonEntries;
  }

  private String getFileNameFromArgs(final ApplicationArguments args) {
    String taxonFile = null;
    if (args.getOptionNames().contains("file")) {
      taxonFile = args.getOptionValues("file").get(0);
    } else {
      taxonFile =
          "/lts/production/tburdett/ena/taxon_name_change/taxname_change_20230414_1551_SAMN.json";
    }

    return taxonFile;
  }
}
