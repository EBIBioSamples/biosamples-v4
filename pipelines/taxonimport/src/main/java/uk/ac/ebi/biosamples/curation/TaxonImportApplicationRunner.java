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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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

  private final BioSamplesClient bioSamplesAapClient;

  @Qualifier("WEBINCLIENT")
  private final BioSamplesClient bioSamplesWebinClient;

  private final AnalyticsService analyticsService;
  private final PipelineFutureCallback pipelineFutureCallback;
  private final ObjectMapper objectMapper;

  public TaxonImportApplicationRunner(
      final BioSamplesClient bioSamplesAapClient,
      final BioSamplesClient bioSamplesWebinClient,
      final AnalyticsService analyticsService,
      final ObjectMapper objectMapper) {
    this.bioSamplesAapClient = bioSamplesAapClient;
    this.bioSamplesWebinClient = bioSamplesWebinClient;
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

    final String taxonDir = getDirectoryNameFromArgs(args);
    final String successDir = taxonDir + File.separator + "processed";
    final String failDir = taxonDir + File.separator + "failed";

    final Queue<File> taxonFiles = getFileListFromDirectory(taxonDir);
    while (!taxonFiles.isEmpty()) {
      final File taxonFile = taxonFiles.remove();
      try {
        LOG.info("Processing file: {}", taxonFile.getName());
        final List<TaxonEntry> taxonEntries = loadTaxonEntries(taxonFile);
        LOG.info("Number of taxon entries to process: {}", taxonEntries.size());
        final long curatedCount = processTaxonEntries(taxonEntries);

        totalProcessed += taxonEntries.size();
        totalCurated += curatedCount;

        LOG.info("Finished processing file: {}", taxonFile.getName());
        moveFile(taxonFile, successDir);
        LOG.info("File moved to {}", successDir);
      } catch (final Exception e) {
        LOG.error("Exception while processing the file: {}", taxonFile.getName());
        moveFile(taxonFile, failDir);
        LOG.warn("File moved to {}", failDir);
      }
    }

    final Instant endTime = Instant.now();
    final long runtime = Duration.between(startTime, endTime).getSeconds();

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

  private long processTaxonEntries(final List<TaxonEntry> taxonEntries) throws Exception {
    long curatedSampleCount = 0;
    for (final TaxonEntry entry : taxonEntries) {
      final Sample sample = getSampleUncurated(entry.getBioSampleAccession());

      final Long oldTaxId = sample.getTaxId();
      final Optional<Attribute> oldOrganism =
          sample.getCharacteristics().stream()
              .filter(c -> c.getType().equalsIgnoreCase("organism"))
              .findFirst();

      if (oldTaxId != entry.getTaxId()
          || !oldOrganism.isPresent()
          || !entry.getNcbiTaxonName().equalsIgnoreCase(oldOrganism.get().getValue())) {
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
    final Queue<File> files;
    try (final Stream<Path> paths = Files.walk(Paths.get(directoryPath), 1)) {
      files =
          paths
              .filter(Files::isRegularFile)
              .map(Path::toFile)
              .collect(Collectors.toCollection(LinkedList::new));
    }
    return files;
  }

  private List<TaxonEntry> loadTaxonEntries(final File taxonFile) {
    final List<TaxonEntry> taxonEntries;
    try {
      taxonEntries = objectMapper.readValue(taxonFile, new TypeReference<List<TaxonEntry>>() {});
    } catch (final IOException e) {
      LOG.error("Failed to process json file", e);
      throw new RuntimeException(
          "Failed to process the json file. Pipeline failed to run. Exiting the pipeline.");
    }

    return taxonEntries;
  }

  private void moveFile(final File target, final String destinationDir) {
    final File destination = new File(destinationDir + File.separator + target.getName());
    final boolean success = target.renameTo(destination);
    if (!success) {
      LOG.error("Failed to move file {} to the directory {}", target.getName(), destinationDir);
    }
  }

  private Sample getSampleUncurated(final String accession) {
    Optional<EntityModel<Sample>> optionalSample =
        bioSamplesAapClient.fetchSampleResource(
            accession, Optional.of(Collections.singletonList("")));

    if (optionalSample.isPresent()) {
      final Sample sample = optionalSample.get().getContent();

      if (sample != null) {
        return sample;
      }
    } else {
      optionalSample =
          bioSamplesWebinClient.fetchSampleResource(
              accession, Optional.of(Collections.singletonList("")));

      if (optionalSample.isPresent()) {
        final Sample sample = optionalSample.get().getContent();

        if (sample != null) {
          return sample;
        }
      }
    }
    LOG.error("Failed to retrieve sample, accession: {}", accession);
    throw new RuntimeException("Failed to retrieve sample, accession: " + accession);
  }

  private void buildAndPersistNewSample(
      final Sample sample,
      final Optional<Attribute> oldOrganism,
      final long oldTaxId,
      final TaxonEntry entry) {
    final Sample newSample;
    final Attribute newOrganism = Attribute.build("organism", entry.getNcbiTaxonName());
    final Set<Attribute> sampleAttributes = sample.getAttributes();

    if (oldOrganism.isPresent()) {
      sampleAttributes.remove(oldOrganism.get());

      LOG.info(
          "Curating sample {} : taxId = {} -> {}  : organism = {} -> {}",
          entry.getBioSampleAccession(),
          oldTaxId,
          entry.getTaxId(),
          oldOrganism.get().getValue(),
          entry.getNcbiTaxonName());
    } else {
      LOG.info(
          "Curating sample {} : taxId = {} : organism = {}",
          entry.getBioSampleAccession(),
          oldTaxId,
          entry.getNcbiTaxonName());
    }

    sampleAttributes.add(newOrganism);
    newSample = Sample.Builder.fromSample(sample).withTaxId(entry.getTaxId()).build();

    if (newSample.getDomain() != null) {
      bioSamplesAapClient.persistSampleResource(newSample);
    } else if (newSample.getWebinSubmissionAccountId() != null) {
      bioSamplesWebinClient.persistSampleResource(newSample);
    } else {
      LOG.info("Sample doesn't have an identity information, skipping " + newSample.getAccession());
    }
  }
}
