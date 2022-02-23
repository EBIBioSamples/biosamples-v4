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
package uk.ac.ebi.biosamples.ena.amr.service;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.BufferedReader;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ena.amr.AmrRunner;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataEntry;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;

@Service
public class EnaAmrDataProcessService {
  private static final Logger log = LoggerFactory.getLogger(EnaAmrDataProcessService.class);
  public static final ConcurrentLinkedQueue<String> failedQueue =
      new ConcurrentLinkedQueue<String>();

  @Autowired
  BioSamplesProperties bioSamplesProperties;

  public void processAmrData(final List<String> lines, final Sample sample, final BioSamplesClient client) {
    /*String[] dilutionMethods = new String[]{"Broth dilution", "Microbroth dilution", "Agar dilution"};
    String[] diffusionMethods = new String[]{"Disc-diffusion", "Neo-sensitabs", "Etest"};*/

    final String accession = sample.getAccession();

    if (!accession.startsWith(AmrRunner.SAMEA) && sample.getData().size() > 0)
      log.info("Not an ENA sample and AMR data already present in sample for " + accession);
    else processAmrData(lines, sample, client, accession);
  }

  public List<String> processAmrLines(BufferedReader bufferedReader) {
    return bufferedReader
        .lines()
        .skip(1)
        .map(this::removeBioSampleId)
        .map(this::dealWithExtraTabs)
        .collect(Collectors.toList());
  }

  private void processAmrData(List<String> lines, Sample sample, BioSamplesClient client, String accession) {
    Set<Map<String, StructuredDataEntry>> tableContent = new HashSet<>();
    StructuredDataTable table = StructuredDataTable.build("self.BiosampleImportENA", null, "AMR", null, tableContent);

    lines.forEach(
        line -> {
          final CsvMapper mapper = new CsvMapper();
          final CsvSchema schema = mapper.schemaFor(Map.class).withColumnSeparator('\t');
          final ObjectReader r = mapper.readerFor(Map.class).with(schema);
          try {
            final Map<String, String> amrEntry = r.readValue(line);
            Map<String, StructuredDataEntry> entry = new HashMap<>();
            for (Map.Entry<String, String> e : amrEntry.entrySet()) {
              entry.put(e.getKey(), StructuredDataEntry.build(e.getValue(), null));
            }

            tableContent.add(entry);
          } catch (final Exception e) {
            log.error("Error in parsing AMR data for sample " + accession);
          }
        });

    Set<StructuredDataTable> tableSet =  Collections.singleton(table);

    if (!lines.isEmpty()) {
      client.persistStructuredData(StructuredData.build(accession, Instant.now(), tableSet));
      log.info("Structured data submitted for sample: " + accession);
    }
  }

  private String removeBioSampleId(final String line) {
    return line.substring(line.indexOf(AmrRunner.TAB) + 1);
  }

  private String dealWithExtraTabs(String line) {
    while (line.endsWith(AmrRunner.TAB)) {
      line = line.substring(0, line.length() - 1);
    }

    return line;
  }
}
