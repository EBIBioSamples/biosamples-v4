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
package uk.ac.ebi.biosamples.curationundo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;

@Component
public class CurationUndoApplicationRunner implements ApplicationRunner {
  private Logger log = LoggerFactory.getLogger(getClass());

  private final BioSamplesClient bioSamplesClient;

  public CurationUndoApplicationRunner(BioSamplesClient bioSamplesClient) {
    this.bioSamplesClient = bioSamplesClient;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    boolean isPassed = true;

    try (AdaptiveThreadPoolExecutor executorService =
        AdaptiveThreadPoolExecutor.create(
            100,
            10000,
            true,
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 2)) {
      Map<String, Future<Void>> futures = new HashMap<>();
      long samplesQueued = 0;
      long startTime = System.currentTimeMillis();
      try {
        for (EntityModel<Sample> sampleResource :
            bioSamplesClient.fetchSampleResourceAll("", Collections.emptyList())) {
          String accession = Objects.requireNonNull(sampleResource.getContent()).getAccession();
          samplesQueued++;
          boolean canary = (samplesQueued % 1000 == 0);
          Callable<Void> task = new CurationUndoCallable(bioSamplesClient, accession, canary);
          futures.put(accession, executorService.submit(task));
          if (canary) {
            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime);
            log.info(
                "PROCESSED: samples:"
                    + samplesQueued
                    + " rate: "
                    + samplesQueued / ((duration / 1000) + 1)
                    + " samples per second");
          }
        }
      } catch (final Exception e) {
        log.error("Pipeline failed to finish successfully", e);
        isPassed = false;
      }
    }
  }
}
