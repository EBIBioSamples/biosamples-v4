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
package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.AccessionFtpUrlPair;
import uk.ac.ebi.biosamples.model.structured.StructuredDataEntry;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;

@Service
public class AmrDataLoaderService {
  private static final Logger log = LoggerFactory.getLogger(AmrDataLoaderService.class);
  private static final String BSD_SAMPLE_PREFIX = "SA";
  private static final String FTP = "ftp";
  private static final String MD_5 = "md5";
  private static final String HTTP = "http://";
  private static final String antibiogram = "\"AMR_ANTIBIOGRAM\"";
  private static final String URL =
      "https://www.ebi.ac.uk/ena/portal/api/search?result=analysis&query=analysis_type="
          + antibiogram
          + "&dataPortal=pathogen&dccDataOnly=false&fields=analysis_accession,country,region,scientific_name,location,sample_accession,tax_id,submitted_ftp,first_public,last_updated&sortFields=scientific_name,country&limit=0";
  private static final String TAB = "\t";

  private Map<String, Set<StructuredDataTable>> loadAmrData() {
    log.info("Loading ENA-AMR data");

    final Map<String, Set<StructuredDataTable>> sampleToAmrMap = new HashMap<>();
    final List<AccessionFtpUrlPair> pairList;

    try {
      pairList = requestHttpAndGetAccessionFtpUrlPairs();

      if (pairList.isEmpty()) {
        log.info(
            "Unable to fetch ENA-AMR Antibiogram data from ENA API, Timed out waiting for connection");
      } else {
        downloadFtpContent(pairList, sampleToAmrMap);
      }
    } catch (final Exception e) {
      log.info("An exception occured while processing AMR data " + e.getMessage());
    }

    return sampleToAmrMap;
  }

  private static List<AccessionFtpUrlPair> requestHttpAndGetAccessionFtpUrlPairs()
      throws Exception {
    final URL enaApiUrl = new URL(AmrDataLoaderService.URL);
    final HttpURLConnection conn = (HttpURLConnection) enaApiUrl.openConnection();
    List<AccessionFtpUrlPair> pairList = new ArrayList<>();

    try {
      if (getResponseFromEnaApi(conn) == 200) {
        pairList = doGetAccessionFtpUrlPairs(enaApiUrl);
      }
    } catch (final Exception e) {
      throw new RuntimeException(e);
    } finally {
      conn.disconnect();
    }

    return pairList;
  }

  private static int getResponseFromEnaApi(final HttpURLConnection conn) throws IOException {
    final int response;

    conn.setRequestMethod("GET");
    conn.connect();
    response = conn.getResponseCode();

    return response;
  }

  private static List<AccessionFtpUrlPair> doGetAccessionFtpUrlPairs(final URL url) {
    final List<AccessionFtpUrlPair> accessionFtpUrlPairs = new ArrayList<>();

    try {
      final BufferedReader bufferedReader = getReader(url);

      bufferedReader
          .lines()
          .skip(1)
          .forEach(line -> accessionFtpUrlPairs.add(getAccessionFtpUrlPair(line)));
    } catch (final IOException e) {
      log.info("Failed to get and parse accession and FTP pairs for URL " + url.toString());
    }

    return accessionFtpUrlPairs;
  }

  private static BufferedReader getReader(final URL url) throws IOException {
    return new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
  }

  private static AccessionFtpUrlPair getAccessionFtpUrlPair(final String line) {
    final StringTokenizer tokenizer = new StringTokenizer(line, TAB);
    final AccessionFtpUrlPair accessionFtpUrlPair = new AccessionFtpUrlPair();

    while (tokenizer.hasMoreTokens()) {
      final String value = tokenizer.nextToken();

      if (value.startsWith(BSD_SAMPLE_PREFIX)) {
        accessionFtpUrlPair.setAccession(value);
      }

      if (value.startsWith(FTP)) {
        dealWithSemicolon(value, accessionFtpUrlPair);
      }
    }

    return accessionFtpUrlPair;
  }

  private static void dealWithSemicolon(
      final String value, final AccessionFtpUrlPair accessionFtpUrlPair) {
    accessionFtpUrlPair.setFtpUrl(HTTP + value);
  }

  private Map<String, Set<StructuredDataTable>> downloadFtpContent(
      final List<AccessionFtpUrlPair> pairList,
      final Map<String, Set<StructuredDataTable>> sampleToAmrMap) {
    pairList.forEach(
        pair -> {
          try {
            final String accession = pair.getAccession();

            if (accession != null) {
              sampleToAmrMap.put(
                  accession, fetchSampleAndProcessAmrData(new URL(pair.getFtpUrl()), accession));
            }
          } catch (final MalformedURLException e) {
            log.info("FTP URL not correctly formed " + pair.getFtpUrl());
          }
        });

    return sampleToAmrMap;
  }

  private Set<StructuredDataTable> fetchSampleAndProcessAmrData(
      final URL url, final String accession) {
    Set<StructuredDataTable> amrData = new HashSet<>();

    try {
      amrData = processAmrData(processAmrLines(getReader(url)), accession);
    } catch (final IOException ioe) {
      log.info("A IO Exception occurrence detected");

      if (amrData.isEmpty()) {
        log.info("Couldn't process AMR data for " + accession);
      }
    }

    return amrData;
  }

  private List<String> processAmrLines(final BufferedReader bufferedReader) {
    return bufferedReader
        .lines()
        .skip(1)
        .map(this::removeBioSampleId)
        .map(this::dealWithExtraTabs)
        .collect(Collectors.toList());
  }

  private Set<StructuredDataTable> processAmrData(
      final List<String> lines, final String accession) {
    final Set<Map<String, StructuredDataEntry>> tableContent = new HashSet<>();
    final StructuredDataTable table =
        StructuredDataTable.build("self.BiosampleImportENA", null, "AMR", null, tableContent);

    lines.forEach(
        line -> {
          final CsvMapper mapper = new CsvMapper();
          final CsvSchema schema = mapper.schemaFor(Map.class).withColumnSeparator('\t');
          final ObjectReader r = mapper.readerFor(Map.class).with(schema);
          try {
            final Map<String, String> amrEntry = r.readValue(line);
            final Map<String, StructuredDataEntry> entry = new HashMap<>();
            for (final Map.Entry<String, String> e : amrEntry.entrySet()) {
              entry.put(e.getKey(), StructuredDataEntry.build(e.getValue(), null));
            }

            tableContent.add(entry);
          } catch (final Exception e) {
            e.printStackTrace();

            log.error("Error in parsing AMR data for sample " + accession);
          }
        });

    return Collections.singleton(table);
  }

  private String removeBioSampleId(final String line) {
    return line.substring(line.indexOf(AmrDataLoaderService.TAB) + 1);
  }

  private String dealWithExtraTabs(String line) {
    while (line.endsWith(AmrDataLoaderService.TAB)) {
      line = line.substring(0, line.length() - 1);
    }

    return line;
  }

  public static void main(final String[] args) {
    final AmrDataLoaderService amrDataLoaderService = new AmrDataLoaderService();

    amrDataLoaderService.loadAmrData();
  }
}
