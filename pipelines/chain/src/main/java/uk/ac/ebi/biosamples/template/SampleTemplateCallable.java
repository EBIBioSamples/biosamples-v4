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

import uk.ac.ebi.biosamples.PipelineSampleCallable;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;

public class SampleTemplateCallable extends PipelineSampleCallable {
  public SampleTemplateCallable(final BioSamplesClient bioSamplesClient, final String domain) {
    super(bioSamplesClient);
  }

  @Override
  public int processSample(final Sample sample) {
    return 1;
  }
}
