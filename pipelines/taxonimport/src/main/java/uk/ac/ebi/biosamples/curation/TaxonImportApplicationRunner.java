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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    long totalCurated = 0;
    long totalProcessed = 0;
    final SampleAnalytics sampleAnalytics = new SampleAnalytics();

    String taxonDir = getDirectoryNameFromArgs(args);
    String successDir = taxonDir + File.separator + "processed";
    String failDir = taxonDir + File.separator + "failed";

    Queue<File> taxonFiles = getFileListFromDirectory(taxonDir);
    while (!taxonFiles.isEmpty()) {
      File taxonFile = taxonFiles.remove();
      try {
        LOG.info("Processing file: {}", taxonFile.getName());
        List<TaxonEntry> taxonEntries = loadTaxonEntries(taxonFile);
        LOG.info("Number of taxon entries to process: {}", taxonEntries.size());
        long curatedCount = processTaxonEntries(taxonEntries);
        totalProcessed += taxonEntries.size();
        totalCurated += curatedCount;

        LOG.info("Finished processing file: {}", taxonFile.getName());
        moveFile(taxonFile, successDir);
        LOG.info("File moved to {}", successDir);
      } catch (Exception e) {
        LOG.error("Exception while processing the file: {}", taxonFile.getName());
        moveFile(taxonFile, failDir);
        LOG.warn("File moved to {}", failDir);
      }
    }

    final Instant endTime = Instant.now();
    long runtime = Duration.between(startTime, endTime).getSeconds();
    LOG.info("Total samples processed {}", totalProcessed);
    LOG.info("Total modified samples {}", totalCurated);
    LOG.info("Pipeline finished at {}", endTime);
    LOG.info("Pipeline total running time {} seconds", runtime);

    final PipelineAnalytics pipelineAnalytics =
        new PipelineAnalytics(
            "taxonimport",
            startTime,
            endTime,
            totalCurated,
            pipelineFutureCallback.getTotalCount());
    sampleAnalytics.setProcessedRecords(totalProcessed);
    analyticsService.persistSampleAnalytics(startTime, sampleAnalytics);
    analyticsService.persistPipelineAnalytics(pipelineAnalytics);
  }

  private long processTaxonEntries(List<TaxonEntry> taxonEntries) throws Exception {
    long curatedSampleCount = 0;
    for (TaxonEntry entry : taxonEntries) {
      Sample sample = getSampleUncurated(entry.getBioSampleAccession());

      Long oldTaxId = sample.getTaxId();
      Optional<Attribute> oldOrganism =
          sample.getCharacteristics().stream()
              .filter(c -> c.getType().equalsIgnoreCase("organism"))
              .findFirst();

      if (oldTaxId != entry.getTaxId() ||
          !oldOrganism.isPresent() ||
          !entry.getNcbiTaxonName().equalsIgnoreCase(oldOrganism.get().getValue())) {
        buildAndPersistNewSample(sample, oldOrganism, oldTaxId, entry);
        curatedSampleCount++;
      }
    }
    return curatedSampleCount;
  }

  private String getDirectoryNameFromArgs(final ApplicationArguments args) {
    String directoryName = null;
    if (args.getOptionNames().contains("dir")) {
      directoryName = args.getOptionValues("dir").get(0);
    } else {
      directoryName = "/nfs/production/cochrane/ena/browser/biosamples/output/report";
    }
    return directoryName;
  }

  private Queue<File> getFileListFromDirectory(final String directoryPath) throws IOException {
    Queue<File> files;
    try (Stream<Path> paths = Files.walk(Paths.get(directoryPath), 1)) {
      files = paths
          .filter(Files::isRegularFile)
          .map(Path::toFile).collect(Collectors.toCollection(LinkedList::new));
    }
    return files;
  }

  private List<TaxonEntry> loadTaxonEntries(final File taxonFile) {
    List<TaxonEntry> taxonEntries;
    try {
      taxonEntries = objectMapper.readValue(taxonFile, new TypeReference<List<TaxonEntry>>() {
      });
    } catch (IOException e) {
      LOG.error("Failed to process json file", e);
      throw new RuntimeException(
          "Failed to process the json file. Pipeline failed to run. Exiting the pipeline.");
    }

    return taxonEntries;
  }

  private void moveFile(File target, String destinationDir) {
    File destination = new File(destinationDir + File.separator + target.getName());
    boolean success = target.renameTo(destination);
    if (!success) {
      LOG.error("Failed to move file {} to the directory {}", target.getName(), destinationDir);
    }
  }

  private Sample getSampleUncurated(String accession) throws Exception {
    Optional<EntityModel<Sample>> optionalSample =
        bioSamplesClient.fetchSampleResource(accession, Optional.of(Collections.singletonList("")));
    if (optionalSample.isPresent()) {
      Sample sample = optionalSample.get().getContent();
      if (sample != null) {
        return sample;
      }
    }
    LOG.error("Failed to retrieve sample, accession: {}", accession);
    throw new RuntimeException("Failed to retrieve sample, accession: " + accession);
  }

  private void buildAndPersistNewSample(Sample sample, Optional<Attribute> oldOrganism, long oldTaxId, TaxonEntry entry) {
    Sample newSample;
    Attribute newOrganism = Attribute.build("organism", entry.getNcbiTaxonName());

    Set<Attribute> sampleAttributes = sample.getAttributes();
    if (oldOrganism.isPresent()) {
      sampleAttributes.remove(oldOrganism.get());
      LOG.info("Curating sample {} : taxId = {} -> {}  : organism = {} -> {}",
          entry.getBioSampleAccession(), oldTaxId, entry.getTaxId(),
          oldOrganism.get().getValue(), entry.getNcbiTaxonName());
    } else {
      LOG.info(
          "Curating sample {} : taxId = {} : organism = {}",
          entry.getBioSampleAccession(), oldTaxId, entry.getNcbiTaxonName());
    }
    sampleAttributes.add(newOrganism);
    newSample = Sample.Builder.fromSample(sample).withTaxId(entry.getTaxId()).build();
    bioSamplesClient.persistSampleResource(newSample);
  }

}
