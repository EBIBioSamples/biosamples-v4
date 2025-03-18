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
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.auth.services.AuthChangeHandler;
import uk.ac.ebi.biosamples.helpdesk.services.*;

// import uk.ac.ebi.biosamples.service.AnalyticsService;

@Component
@Slf4j
public class HelpdeskActionApplicationRunner implements ApplicationRunner {
  @Autowired SampleChecklistComplianceHandlerEVA sampleChecklistComplianceHandlerEVA;
  @Autowired SampleStatusUpdater sampleStatusUpdater;
  @Autowired AuthChangeHandler authChangeHandler;
  @Autowired SampleRelationshipHandler sampleRelationshipHandler;
  @Autowired SampleExternalReferenceHandler sampleExternalReferenceHandler;
  @Autowired SampleRestoreIPK sampleRestoreIPK;

  @Override
  public void run(ApplicationArguments args) {
    // authChangeHandler.parseFileAndProcessSampleAuthentication();
    // sampleChecklistComplianceHandlerEVA.samnSampleGeographicLocationAttributeUpdateFromFile();
    try {
      final List<String> accessions =
          sampleRestoreIPK.parseInput("C:\\Users\\dgupta\\IPK_samples.xlsx");

      log.info("Number of accessions to be handled are " + accessions.size());

      final List<String> updateResults =
          accessions.stream()
              .map(
                  accession ->
                      sampleRestoreIPK.restoreSample(accession)
                          ? accession + " updated"
                          : accession + " not updated")
              .toList();

      Files.write(
          Paths.get("updateResults_2.txt"),
          updateResults,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (Exception e) {
      log.info("Operation failed");
      e.printStackTrace();

      throw new RuntimeException(e);
    }
    /*final List<String> accessions =
        sampleStatusUpdater.parseFileAndGetSampleAccessionList(
            "C:\\Users\\dgupta\\AtlantECO-samples-to-suppress.txt");

    sampleStatusUpdater.processSamples(accessions);*/
    try {
      /*sampleRelationshipHandler.processFile(
      "C:\\Users\\dgupta\\ParentChild_Biosamples_mapping_clean.xlsx");*/
      // sampleChecklistComplianceHandlerEVA.samnSampleGeographicLocationAttributeUpdateFromFile();
      // sampleExternalReferenceHandler.processSample("SAMEA115414646");

      // sampleStatusUpdater.makeFilteredSamplesPrivate();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
