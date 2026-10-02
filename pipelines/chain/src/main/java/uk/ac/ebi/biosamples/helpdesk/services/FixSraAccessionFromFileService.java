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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesConstants;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Attribute;
import uk.ac.ebi.biosamples.core.model.Sample;

/**
 * Reads a tab-separated file (sampleName accession, e.g. TARACORAL1 SAMEA121392853), fetches
 * samples by accession via V2 in batches, removes the wrongly set SRA accession attribute, and
 * persists updated samples via persistSampleResourceV2 in batches.
 */
@Service
public class FixSraAccessionFromFileService {

  private static final Logger log = LoggerFactory.getLogger(FixSraAccessionFromFileService.class);

  private static final int BATCH_SIZE = 200;

  private final BioSamplesClient bioSamplesClient;

  public FixSraAccessionFromFileService(
      @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesClient) {
    this.bioSamplesClient = bioSamplesClient;
  }

  /**
   * Reads the file (lines: name\taccession), fetches samples in batches, removes the SRA accession
   * attribute from each, and persists via persistSampleResourceV2 in batches.
   *
   * @param inputFilePath path to tab-separated file (sampleName accession)
   */
  public void fixSraAccessionFromFile(final String inputFilePath) {
    final List<String> accessions = readAccessionsFromFile(inputFilePath);
    log.info("Read {} accessions from {}", accessions.size(), inputFilePath);

    int totalPersisted = 0;
    for (int start = 0; start < accessions.size(); start += BATCH_SIZE) {
      final int end = Math.min(start + BATCH_SIZE, accessions.size());
      final List<String> batchAccessions = accessions.subList(start, end);
      log.info("Processing batch {}-{} of {}", start + 1, end, accessions.size());

      final Map<String, Sample> fetched =
          bioSamplesClient.fetchSampleResourcesByAccessionsV2(batchAccessions);
      final List<Sample> toPersist = new ArrayList<>();

      for (final String accession : batchAccessions) {
        final Sample sample = fetched.get(accession);
        if (sample == null) {
          log.warn("Sample not found: {}", accession);
          continue;
        }

        Set<Attribute> attributes = sample.getAttributes();
        if (attributes == null) {
          attributes = new TreeSet<>();
        } else {
          attributes = new TreeSet<>(attributes);
        }

        final boolean removed =
            attributes.removeIf(
                attr ->
                    attr.getType() != null
                        && attr.getType().equalsIgnoreCase(BioSamplesConstants.SRA_ACCESSION));

        if (!removed) {
          log.debug("No SRA accession attribute to remove for {}", accession);
          continue;
        }

        final Sample updated = Sample.Builder.fromSample(sample).withAttributes(attributes).build();
        toPersist.add(updated);
      }

      if (!toPersist.isEmpty()) {
        bioSamplesClient.persistSampleResourceV2(toPersist);
        totalPersisted += toPersist.size();
        log.info(
            "Persisted batch of {} updated samples (total so far: {})",
            toPersist.size(),
            totalPersisted);
      }
    }

    log.info("Finished fixing SRA accession for {} samples", totalPersisted);
  }

  private static List<String> readAccessionsFromFile(final String inputFilePath) {
    final List<String> accessions = new ArrayList<>();
    try (final BufferedReader br = new BufferedReader(new FileReader(inputFilePath))) {
      String line;
      while ((line = br.readLine()) != null) {
        final String trimmed = line.trim();
        if (trimmed.isEmpty()) {
          continue;
        }
        final int tab = trimmed.indexOf('\t');
        final String accession = tab >= 0 ? trimmed.substring(tab + 1).trim() : trimmed;
        if (!accession.isEmpty()) {
          accessions.add(accession);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to read file: " + inputFilePath, e);
    }
    return accessions;
  }
}
