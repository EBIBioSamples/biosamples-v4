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

import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelineApplicationRunner;
import uk.ac.ebi.biosamples.PipelineSampleCallable;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;

// import uk.ac.ebi.biosamples.service.AnalyticsService;

@Component
public class PipelineTemplateApplicationRunner extends PipelineApplicationRunner {
  private static final String PIPELINE_NAME = "SAMPLE_TEST";
  private String domain;

  public PipelineTemplateApplicationRunner(
      BioSamplesClient bioSamplesClient, PipelinesProperties pipelinesProperties /*,
                                             AnalyticsService analyticsService*/) {
    super(bioSamplesClient, pipelinesProperties /*, analyticsService*/);
  }

  @Override
  public void loadPreConfiguration() {
    this.domain = "self.testDomain";
    LOG.info("Loading pre configurations for {}", PIPELINE_NAME);
  }

  @Override
  public PipelineSampleCallable getNewCallableClassInstance() {
    return new SampleTemplateCallable(bioSamplesClient, domain);
  }

  protected String getPipelineName() {
    return PIPELINE_NAME;
  }
}
