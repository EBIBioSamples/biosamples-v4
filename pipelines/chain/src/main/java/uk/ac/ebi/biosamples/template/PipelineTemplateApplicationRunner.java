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
package uk.ac.ebi.biosamples.template;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.auth.AuthChangeHandler;
import uk.ac.ebi.biosamples.helpdesk.SampleChecklistComplaintMaker;
import uk.ac.ebi.biosamples.helpdesk.SampleStatusUpdater;

// import uk.ac.ebi.biosamples.service.AnalyticsService;

@Component
public class PipelineTemplateApplicationRunner implements ApplicationRunner {
  @Autowired SampleChecklistComplaintMaker sampleChecklistComplaintMaker;
  @Autowired SampleStatusUpdater sampleStatusUpdater;
  @Autowired AuthChangeHandler authChangeHandler;

  @Override
  public void run(ApplicationArguments args) {
    // authChangeHandler.parseCsvAndProcessSampleAuthentication();
    sampleChecklistComplaintMaker.samnSampleGeographicLocationAttributeUpdateFromFile();
    /*final List<String> accessions =
        sampleStatusUpdater.parseFileAndGetSampleAccessionList(
            "C:\\Users\\dgupta\\AtlantECO-samples-to-suppress.txt");

    sampleStatusUpdater.processSamples(accessions);*/
  }
}
