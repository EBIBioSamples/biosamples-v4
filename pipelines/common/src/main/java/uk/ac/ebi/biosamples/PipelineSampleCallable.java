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

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Sample;

public abstract class PipelineSampleCallable implements Callable<PipelineResult> {
  protected final Logger LOG = LoggerFactory.getLogger(getClass());

  protected Sample sample;
  private final BioSamplesClient bioSamplesClient;

  public PipelineSampleCallable(final BioSamplesClient bioSamplesClient) {
    this.bioSamplesClient = bioSamplesClient;
  }

  @Override
  public PipelineResult call() {
    boolean success = true;
    int appliedCurations = 0;
    try {
      appliedCurations = processSample(sample);
    } catch (final Exception e) {
      success = false;
      LOG.error("Failed to add curation on sample: " + sample.getAccession(), e);
    }
    return new PipelineResult(sample.getAccession(), appliedCurations, success);
  }

  PipelineSampleCallable withSample(final Sample sample) {
    this.sample = sample;
    return this;
  }

  protected abstract int processSample(Sample sample) throws Exception;
}
