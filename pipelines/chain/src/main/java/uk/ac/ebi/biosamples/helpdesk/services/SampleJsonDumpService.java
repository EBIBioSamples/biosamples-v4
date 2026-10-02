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
package uk.ac.ebi.biosamples.helpdesk.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Sample;

/** Shared helper to dump samples as JSON lines from accession batches. */
@Service
public class SampleJsonDumpService {
  private static final Logger log = LoggerFactory.getLogger(SampleJsonDumpService.class);

  private final BioSamplesClient webinClient;
  private final ObjectMapper objectMapper;

  public SampleJsonDumpService(
      @Qualifier("WEBINCLIENT") final BioSamplesClient webinClient,
      final ObjectMapper objectMapper) {
    this.webinClient = webinClient;
    this.objectMapper = objectMapper;
  }

  /**
   * Fetches a batch of accessions via V2 with retry and appends full sample JSON lines.
   *
   * @param accessionBatch accession batch (order preserved in output)
   * @param writer target output writer
   * @param maxRetries maximum number of attempts for the fetch call
   * @return number of samples written
   */
  public int writeSamplesByAccessionBatchWithRetry(
      final List<String> accessionBatch, final BufferedWriter writer, final int maxRetries) {
    if (accessionBatch.isEmpty()) {
      return 0;
    }

    final Map<String, Sample> fetched = fetchBatchWithRetry(accessionBatch, maxRetries);
    int written = 0;

    try {
      for (final String accession : accessionBatch) {
        final Sample sample = fetched.get(accession);
        if (sample == null) {
          log.warn("No sample returned for accession {}", accession);
          continue;
        }

        writer.write(toJson(sample));
        writer.newLine();
        written++;
      }
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException("Failed writing dumped samples", e);
    }

    return written;
  }

  private Map<String, Sample> fetchBatchWithRetry(
      final List<String> accessionBatch, final int maxRetries) {
    RuntimeException last = null;
    final int attempts = Math.max(1, maxRetries);

    for (int attempt = 1; attempt <= attempts; attempt++) {
      try {
        return webinClient.fetchSampleResourcesByAccessionsV2(accessionBatch);
      } catch (RuntimeException e) {
        last = e;
        if (attempt < attempts) {
          final long backoffMs = Math.min(30000L, 1000L * (1L << (attempt - 1)));
          log.warn(
              "Failed fetching batch (attempt {}/{}). Retrying in {} ms. Error: {}",
              attempt,
              attempts,
              backoffMs,
              e.getMessage());
          sleep(backoffMs);
        }
      }
    }

    throw new RuntimeException(
        "Failed fetching accession batch after " + attempts + " attempts", last);
  }

  private String toJson(final Sample sample) {
    try {
      return objectMapper.writeValueAsString(sample);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize sample to JSON: " + sample.getAccession(), e);
    }
  }

  private static void sleep(final long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted during retry backoff", e);
    }
  }
}
