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
package uk.ac.ebi.biosamples.curami.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

public class DataLoader {
  private static final Logger LOG = LoggerFactory.getLogger(DataLoader.class);

  private static final SortedSet<String> POPULAR_ATTRIBUTES = new TreeSet<>();
  private static final Set<String> ABBREVIATIONS = new HashSet<>();
  private static final Map<String, String> CURATIONS = new HashMap<>();

  public SortedSet<String> getPopularAttributes() {
    return POPULAR_ATTRIBUTES;
  }

  public Set<String> getAbbreviations() {
    return ABBREVIATIONS;
  }

  public Map<String, String> getCurations() {
    return CURATIONS;
  }

  public void loadDataFromClassPathResource() {
    loadPopularAttributes("attributes.csv");
    loadAbbreviations("abbreviations.csv");
    loadCurations("curations.csv");
  }

  private void loadPopularAttributes(final String filePath) {
    try {
      final Reader in =
          new BufferedReader(
              new InputStreamReader(new ClassPathResource(filePath).getInputStream()));
      final Iterable<CSVRecord> records =
          CSVFormat.DEFAULT.withHeader("ATTRIBUTE", "COUNT").parse(in);
      for (final CSVRecord record : records) {
        final String attribute = record.get("ATTRIBUTE");
        POPULAR_ATTRIBUTES.add(attribute);
      }
    } catch (final IOException e) {
      LOG.error("Failed to load CSV file at: " + filePath, e);
    }
  }

  private void loadAbbreviations(final String filePath) {
    try {
      final Reader in =
          new BufferedReader(
              new InputStreamReader(new ClassPathResource(filePath).getInputStream()));
      final Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader("ATTRIBUTE").parse(in);
      for (final CSVRecord record : records) {
        final String attribute = record.get("ATTRIBUTE");
        ABBREVIATIONS.add(attribute);
      }
    } catch (final IOException e) {
      LOG.error("Failed to load CSV file at: " + filePath, e);
    }
  }

  private void loadCurations(final String filePath) {
    try {
      final Reader in =
          new BufferedReader(
              new InputStreamReader(new ClassPathResource(filePath).getInputStream()));
      final Iterable<CSVRecord> records =
          CSVFormat.DEFAULT.withHeader("ATTRIBUTE", "CURATION").parse(in);
      for (final CSVRecord record : records) {
        final String attribute = record.get("ATTRIBUTE");
        final String curation = record.get("CURATION");
        CURATIONS.put(attribute, curation);
      }
    } catch (final IOException e) {
      LOG.error("Failed to load CSV file at: " + filePath, e);
    }
  }
}
