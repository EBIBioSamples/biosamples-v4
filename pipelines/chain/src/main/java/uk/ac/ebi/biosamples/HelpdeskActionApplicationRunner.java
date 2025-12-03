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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.auth.services.AuthChangeHandler;
import uk.ac.ebi.biosamples.auth.services.SamplesCrawlerAuthChangeHandler;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.SampleStatus;
import uk.ac.ebi.biosamples.helpdesk.services.*;

@Component
@Slf4j
public class HelpdeskActionApplicationRunner implements ApplicationRunner {

  @Autowired SampleChecklistComplianceHandlerEVA sampleChecklistComplianceHandlerEVA;
  @Autowired SampleStatusUpdater sampleStatusUpdater;
  @Autowired AuthChangeHandler authChangeHandler;
  @Autowired SampleRelationshipHandler sampleRelationshipHandler;
  @Autowired SampleExternalReferenceHandler sampleExternalReferenceHandler;
  @Autowired SampleRestoreIPK sampleRestoreIPK;
  @Autowired SamplesCrawlerAuthChangeHandler samplesCrawlerAuthChangeHandler;

  @Override
  public void run(ApplicationArguments args) {
    try {
      final String action =
          args.containsOption("action") ? args.getOptionValues("action").get(0) : "";

      switch (action) {
        case "authChangeHandlerStaticSampleList" -> {
          authChangeHandler.parseListOfSamplesAndProcessSampleAuthentication();
        }

        case "evaComplianceUpdate" -> {
          sampleChecklistComplianceHandlerEVA.updateSamnSampleGeographicLocationFromFile();
        }

        case "restoreIPKSamples" -> {
          final List<String> accessions =
              sampleRestoreIPK.parseInput("C:\\Users\\dgupta\\IPK_samples_3.list");

          log.info("Number of accessions to be handled are {}", accessions.size());

          final List<String> updateResults =
              accessions.stream()
                  .map(
                      accession ->
                          sampleRestoreIPK.restoreSample(accession)
                              ? accession + " updated"
                              : accession + " not updated")
                  .toList();

          Files.write(
              Paths.get("updateResults_3.txt"),
              updateResults,
              StandardOpenOption.CREATE,
              StandardOpenOption.TRUNCATE_EXISTING);
        }

        case "changeStatusOfSamplesFromFile" -> {
          final List<String> accessions =
              sampleStatusUpdater.parseFileAndGetSampleAccessionList(
                  "C:\\Users\\dgupta\\AtlantECO-samples-to-suppress.txt");

          sampleStatusUpdater.processSamples(accessions, null);
        }

        case "updateSampleRelationships" -> {
          sampleRelationshipHandler.processFile(
              "C:\\Users\\dgupta\\ParentChild_Biosamples_mapping_clean.xlsx");
        }

        case "addExternalReferenceCurations" -> {
          sampleExternalReferenceHandler.processSample("SAMEA115414646");
        }

        case "makeSamplesPrivate" -> {
          sampleStatusUpdater.makeFilteredSamplesPrivate();
        }

        case "makeSamplesPublicFromStaticSamplesList" -> {
          final List<String> toBePublicSampleAccessions =
              sampleStatusUpdater.parseFileAndGetSampleAccessionList(
                  "C:\\Users\\dgupta\\biosamples_ids_morphic_forPublic.txt");

          sampleStatusUpdater.processSamples(toBePublicSampleAccessions, SampleStatus.PUBLIC);
        }

        case "crawlSamplesAndChangeAuthInfoFromAAPToWebin" -> {
          final String domain =
              args.containsOption("domain") ? args.getOptionValues("domain").get(0) : null;
          final String webinId =
              args.containsOption("webinId") ? args.getOptionValues("webinId").get(0) : null;
          final String releaseFlag =
              args.containsOption("release") ? args.getOptionValues("release").get(0) : null;
          final String file =
              args.containsOption("file") ? args.getOptionValues("file").get(0) : null;
          final boolean release = releaseFlag != null && releaseFlag.equalsIgnoreCase("true");

          if (webinId != null && domain != null) {
            if (file == null) {
              for (EntityModel<Sample> sample :
                  samplesCrawlerAuthChangeHandler.getSamples(domain)) {
                samplesCrawlerAuthChangeHandler.handleAuth(sample, domain, webinId, release);
              }
            } else {
              final List<String> accessions = readAccessions(file);

              if (accessions.isEmpty()) {
                log.warn("No accessions found in file {}", file);
                return;
              }

              int threads = 20;
              int batchSize = 1000;
              int total = accessions.size();
              int processed = 0;
              int successCount = 0;
              int failCount = 0;
              final ExecutorService executorService = Executors.newFixedThreadPool(threads);

              log.info(
                  "Starting auth change handling for {} samples using {} threads", total, threads);

              try {
                for (final List<String> batch : partition(accessions, batchSize)) {
                  final List<Future<Boolean>> futures = new ArrayList<>();

                  for (final String accession : batch) {
                    futures.add(
                        executorService.submit(
                            () -> {
                              try {
                                final EntityModel<Sample> sampleEntityModel =
                                    samplesCrawlerAuthChangeHandler.getSample(accession);

                                if (sampleEntityModel != null) {
                                  samplesCrawlerAuthChangeHandler.handleAuth(
                                      sampleEntityModel, domain, webinId, release);

                                  return true;
                                } else {
                                  log.debug("Sample not found for {}", accession);

                                  return false;
                                }
                              } catch (final Exception e) {
                                log.error("Error processing sample {}", accession, e);

                                return false;
                              }
                            }));
                  }

                  for (final Future<Boolean> future : futures) {
                    try {
                      if (Boolean.TRUE.equals(future.get())) {
                        successCount++;
                      } else {
                        failCount++;
                      }
                    } catch (final Exception e) {
                      failCount++;

                      log.error("Task execution failed", e);
                    }

                    processed++;
                  }

                  log.info(
                      "Processed {} / {} samples (success: {}, failed: {})",
                      processed,
                      total,
                      successCount,
                      failCount);
                }
              } finally {
                // Stop accepting new tasks
                executorService.shutdown();

                try {
                  if (!executorService.awaitTermination(2, TimeUnit.HOURS)) {
                    log.warn("Timeout waiting for tasks to finish, forcing shutdown...");

                    executorService.shutdownNow();
                  }
                } catch (final InterruptedException e) {
                  log.error("Interrupted while waiting for executor shutdown", e);

                  executorService.shutdownNow();
                  Thread.currentThread().interrupt();
                }
              }
            }
          } else {
            log.info("Please provide a valid AAP domain and a valid webin submission account ID");
          }
        }

        default -> log.warn("No valid --action argument provided. Nothing will run.");
      }

    } catch (final Exception e) {
      log.error("Operation failed", e);
      throw new RuntimeException(e);
    }
  }

  /** Splits list into batches */
  private static <T> List<List<T>> partition(List<T> list, int size) {
    List<List<T>> parts = new ArrayList<>();

    for (int i = 0; i < list.size(); i += size) {
      parts.add(list.subList(i, Math.min(i + size, list.size())));
    }

    return parts;
  }

  private static List<String> readAccessions(String file) {
    final List<String> accessions = new ArrayList<>();

    try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;

      while ((line = reader.readLine()) != null) {
        accessions.add(line);
      }
    } catch (IOException e) {
      log.error("Error reading sample list file", e);
      throw new RuntimeException(e);
    }
    return accessions;
  }
}
