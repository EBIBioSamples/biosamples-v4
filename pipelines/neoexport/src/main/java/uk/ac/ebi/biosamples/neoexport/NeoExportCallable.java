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
package uk.ac.ebi.biosamples.neoexport;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.neo4j.model.NeoSample;
import uk.ac.ebi.biosamples.neo4j.repo.NeoSampleRepository;

public class NeoExportCallable implements Callable<PipelineResult> {
  private static final Logger LOG = LoggerFactory.getLogger(NeoExportCallable.class);
  static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<>();

  private final Sample sample;
  private final NeoSampleRepository neoSampleRepository;

  public NeoExportCallable(NeoSampleRepository neoSampleRepository, Sample sample) {
    this.neoSampleRepository = neoSampleRepository;
    this.sample = sample;
  }

  @Override
  public PipelineResult call() {
    try {
      NeoSample neoSample = NeoSample.build(sample);
      neoSampleRepository.loadSample(neoSample);
    } catch (Exception e) {
      failedQueue.add(sample.getAccession());
      LOG.error("Failed to load sample: " + sample.getAccession(), e);
    }
    return new PipelineResult(sample.getAccession(), 0, true);
  }
}
