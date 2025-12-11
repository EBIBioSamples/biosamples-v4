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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
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
          final String file =
              args.containsOption("file") ? args.getOptionValues("file").get(0) : null;

          if (file != null) {
            final List<String> accessions =
                sampleStatusUpdater.parseFileAndGetSampleAccessionList(file);

            sampleStatusUpdater.processSamples(accessions, SampleStatus.PUBLIC);
          } else {
            throw new RuntimeException("File is not provided");
          }
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

          if (webinId != null && domain != null) {
            for (EntityModel<Sample> sample : samplesCrawlerAuthChangeHandler.getSamples(domain)) {
              samplesCrawlerAuthChangeHandler.handleAuth(sample, domain, webinId);
            }
          } else {
            log.info("Please provide a valid AAP domain and a valid webin submission account ID");
          }
        }

        default -> {
          log.warn("No valid --action argument provided. Nothing will run.");
        }
      }

    } catch (final Exception e) {
      log.error("Operation failed", e);
      throw new RuntimeException(e);
    }
  }
}
