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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Attribute;
import uk.ac.ebi.biosamples.core.model.Sample;

@Component
public class SamplesEnaSpatiotemporalComplainceHandler {

  private static final Logger log =
      LoggerFactory.getLogger(SamplesEnaSpatiotemporalComplainceHandler.class);

  private static final String GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA =
      "geographic location (country and/or sea)";
  private static final String GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY =
      "geographic location (region and locality)";
  private static final String COLLECTION_DATE = "collection_date";
  private static final String COLLECTION_DATE_WITHOUT_UNDERSCORE = "collection date";
  private static final String NCBI_MIRRORING_WEBIN_ID = "Webin-842";
  private final List<String> unknowns = List.of("Unknown", "missing");

  private final BioSamplesClient bioSamplesWebinClient;
  private final PipelinesProperties pipelinesProperties;

  public SamplesEnaSpatiotemporalComplainceHandler(
      @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesWebinClient,
      final PipelinesProperties pipelinesProperties) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.pipelinesProperties = pipelinesProperties;
  }

  private void processSample(final String accession) {
    log.info("Processing sample: {}", accession);

    final Optional<EntityModel<Sample>> optionalSampleEntityModel =
        bioSamplesWebinClient.fetchSampleResource(accession, false);

    if (optionalSampleEntityModel.isPresent()) {
      handleGeographicLocationAndCollectionDate(optionalSampleEntityModel.get().getContent());
    } else {
      log.warn("Sample not found: {}", accession);
    }
  }

  private void handleGeographicLocationAndCollectionDate(final Sample sample) {
    if (sample == null) {
      log.warn("Sample object is null, skipping processing");
      return;
    }

    final String accession = sample.getAccession();
    final Set<Attribute> attributes = new HashSet<>(sample.getAttributes());

    // Always read geo_loc_name. Single value -> country/sea only; "Country: Region" -> country/sea
    // + region/locality
    String countryAndOrSea = "not provided";
    String regionAndLocality = "not provided";

    final Optional<Attribute> geoLocOptional =
        attributes.stream().filter(attr -> "geo_loc_name".equals(attr.getType())).findFirst();

    if (geoLocOptional.isPresent()) {
      final String value = geoLocOptional.get().getValue();
      log.info(
          "geo_loc_name attribute present in sample {}: {}",
          accession,
          value.isEmpty() ? "empty" : value);

      if (!value.isEmpty() && !unknowns.contains(value)) {
        final List<String> parts = splitGeoLoc(value.trim());
        final String countryAndOfSea = parts.get(0);

        countryAndOrSea =
            countryAndOfSea.isEmpty()
                    || unknowns.stream()
                        .anyMatch(unknown -> unknown.equalsIgnoreCase(countryAndOfSea))
                ? "not provided"
                : parts.get(0);
        regionAndLocality =
            parts.size() > 1 && !parts.get(1).isEmpty() ? parts.get(1) : "not provided";
      }
    } else {
      log.info(
          "geo_loc_name attribute not present for sample {}, will set as 'not provided'",
          accession);
    }

    attributes.removeIf(
        attribute -> attribute.getType().equals(GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA));
    attributes.removeIf(
        attribute -> attribute.getType().equals(GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY));
    attributes.add(Attribute.build(GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA, countryAndOrSea));
    attributes.add(Attribute.build(GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY, regionAndLocality));
    log.info(
        "Set '{}' to '{}' and '{}' to '{}' for sample {}",
        GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA,
        countryAndOrSea,
        GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY,
        regionAndLocality,
        accession);

    // Handle collection date
    final List<Attribute> collectionDates =
        attributes.stream().filter(attr -> COLLECTION_DATE.equals(attr.getType())).toList();
    String collectionDateValue = "not provided";

    if (collectionDates.size() == 1) {
      final Attribute collectionDate = collectionDates.get(0);
      final String collectionDateAttributeValue = collectionDate.getValue();

      collectionDateValue =
          collectionDateAttributeValue == null
                  || unknowns.stream()
                      .anyMatch(unknown -> unknown.equalsIgnoreCase(collectionDateAttributeValue))
              ? "not provided"
              : collectionDateAttributeValue;
    }

    attributes.removeIf(
        attribute -> attribute.getType().equalsIgnoreCase(COLLECTION_DATE_WITHOUT_UNDERSCORE));
    attributes.add(Attribute.build(COLLECTION_DATE_WITHOUT_UNDERSCORE, collectionDateValue));

    log.info("Set collection_date for sample {}: {}", accession, collectionDateValue);

    // Persist updated sample
    final Sample updatedSample =
        Sample.Builder.fromSample(sample).withAttributes(attributes).build();
    try {
      bioSamplesWebinClient.persistSampleResource(updatedSample);

      log.info("Successfully persisted sample {} using WEBIN client", accession);
    } catch (Exception e) {
      log.error("Failed to persist sample {} using WEBIN client", accession, e);
    }
  }

  /**
   * Updates SAMN samples for ENA compliance from a file. Reads each sample's geo_loc_name: if a
   * single value, sets "geographic location (country and/or sea)"; if "Country: Region" format
   * (e.g. "USA:Virginia" or "China: Jilong, Tibet"), sets country/sea and "geographic location
   * (region and locality)" accordingly.
   *
   * @param filePath path to file containing SAMN accessions (one per line or embedded in text)
   */
  public void updateSamnSampleGeographicLocationFromFile(final String filePath) {
    final Pattern pattern = Pattern.compile("SAMN\\d+");
    final Set<String> samnAccessions = new HashSet<>();

    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = reader.readLine()) != null) {
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
          samnAccessions.add(matcher.group());
        }
      }
    } catch (IOException e) {
      log.error("Error reading sample list file: {}", filePath, e);
      throw new RuntimeException("Failed to read file: " + filePath, e);
    }

    log.info("Found {} SAMN accessions to process", samnAccessions.size());
    samnAccessions.forEach(this::processSample);
  }

  public static List<String> splitGeoLoc(final String input) {
    List<String> parts = new ArrayList<>();
    int index = input.indexOf(':');

    if (index != -1) {
      parts.add(input.substring(0, index).trim()); // country
      parts.add(input.substring(index + 1).trim()); // city
    } else {
      parts.add(input.trim());
      parts.add("");
    }

    return parts;
  }
}
