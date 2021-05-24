/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.curation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.PipelineResult;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.Sample;

public class SampleCuramiCallable implements Callable<PipelineResult> {
  private static final Logger LOG = LoggerFactory.getLogger(SampleCuramiCallable.class);
  static final ConcurrentLinkedQueue<String> failedQueue = new ConcurrentLinkedQueue<>();

  private final Sample sample;
  private final BioSamplesClient bioSamplesClient;
  private final String domain;
  private final Map<String, String> curationRules;

  public SampleCuramiCallable(
      BioSamplesClient bioSamplesClient,
      Sample sample,
      String domain,
      Map<String, String> curationRules) {
    this.bioSamplesClient = bioSamplesClient;
    this.sample = sample;
    this.domain = domain;
    this.curationRules = curationRules;
  }

  @Override
  public PipelineResult call() {
    int appliedCurations = 0;
    boolean success = true;
    try {
      List<Curation> curations = getRuleBasedCurations(sample);
      if (!curations.isEmpty()) {
        LOG.info("{} curations added for sample {}", curations.size(), sample.getAccession());
      }
      appliedCurations = curations.size();
    } catch (Exception e) {
      success = false;
      failedQueue.add(sample.getAccession());
      LOG.error("Failed to add curation on sample: " + sample.getAccession(), e);
    }
    return new PipelineResult(sample.getAccession(), appliedCurations, success);
  }

  private List<Curation> getRuleBasedCurations(Sample sample) {
    SortedSet<Attribute> attributes = sample.getAttributes();
    List<Curation> curations = new ArrayList<>();
    for (Attribute a : attributes) {
      // @here ignoring empty values is wrong. This has done as a workaround as pipeline fails
      // if we
      // post empty
      // values to curation endpoint. Curation endpoint translate empty values as null values
      // and
      // throw an exception.
      if (curationRules.containsKey(a.getType()) && !a.getValue().isEmpty()) {
        Curation curation =
            Curation.build(
                Attribute.build(a.getType(), a.getValue(), a.getTag(), a.getIri(), a.getUnit()),
                Attribute.build(
                    curationRules.get(a.getType()),
                    a.getValue(),
                    a.getTag(),
                    a.getIri(),
                    a.getUnit()));
        LOG.info("New curation found {}", curation);
        bioSamplesClient.persistCuration(sample.getAccession(), curation, domain, false);
        curations.add(curation);
      }
    }

    return curations;
  }
}
