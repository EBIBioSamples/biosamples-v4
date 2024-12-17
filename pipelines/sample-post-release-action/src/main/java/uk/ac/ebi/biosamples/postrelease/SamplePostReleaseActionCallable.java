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
package uk.ac.ebi.biosamples.postrelease;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SampleStatus;

public class SamplePostReleaseActionCallable implements Callable<Boolean> {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final Sample sample;
  private final BioSamplesClient bioSamplesWebinClient;
  static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<>();

  SamplePostReleaseActionCallable(
      final BioSamplesClient bioSamplesWebinClient, final Sample sample) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.sample = sample;
  }

  @Override
  public Boolean call() {
    boolean success = true;
    final SampleStatus sampleStatus = sample.getStatus();

    if (sampleStatus == SampleStatus.PRIVATE) {
      success = makeSampleStatusPublic();
    } else {
      log.info(sample.getAccession() + " is already public, no change required");
    }

    return success;
  }

  private boolean makeSampleStatusPublic() {
    final String sampleAccession = sample.getAccession();
    boolean success = true;

    try {
      bioSamplesWebinClient.persistSampleResource(buildSampleWithPublicStatus());

      log.info(sampleAccession + " is public now");
    } catch (final Exception e) {
      log.info(sampleAccession + " failed to make public");

      failedQueue.add(sampleAccession);
      success = false;
    }

    return success;
  }

  private Sample buildSampleWithPublicStatus() {
    return Sample.Builder.fromSample(sample).withStatus(SampleStatus.PUBLIC).build();
  }
}
