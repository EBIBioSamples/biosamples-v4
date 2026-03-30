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
public class SampleChecklistComplianceHandlerEVA {
  private static final Logger log =
      LoggerFactory.getLogger(SampleChecklistComplianceHandlerEVA.class);

  private static final String GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA =
      "geographic location (country and/or sea)";
  private static final String GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY =
      "geographic location (region and locality)";
  private static final String COLLECTION_DATE = "collection_date";
  private static final String COLLECTION_DATE_WITHOUT_UNDERSCORE = "collection date";

  private final BioSamplesClient bioSamplesWebinClient;
  private final PipelinesProperties pipelinesProperties;

  public SampleChecklistComplianceHandlerEVA(
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

    // Handle geographic location
    final Optional<Attribute> geoLocOptional =
        attributes.stream().filter(attr -> "geo_loc_name".equals(attr.getType())).findFirst();

    if (geoLocOptional.isPresent()) {
      Attribute geoLoc = geoLocOptional.get();
      String value = geoLoc.getValue();

      log.info(
          "geo_loc_name attribute present in sample {}: {}",
          accession,
          value.isEmpty() ? "empty" : value);

      if (!value.isEmpty()) {
        attributes.removeIf(
            attribute -> attribute.getType().equals(GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA));
        attributes.removeIf(
            attribute -> attribute.getType().equals(GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY));

        attributes.add(
            Attribute.build(
                GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA,
                value,
                geoLoc.getTag(),
                Collections.emptyList(),
                geoLoc.getUnit()));

        log.info(
            "Set '{}' attribute for sample {}", GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA, accession);
      } else {
        attributes.removeIf(
            attribute -> attribute.getType().equals(GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA));
        attributes.removeIf(
            attribute -> attribute.getType().equals(GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY));

        attributes.add(Attribute.build(GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA, "not provided"));

        log.info(
            "geo_loc_name empty, set '{}' as 'not provided' for sample {}",
            GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA,
            accession);
      }
    } else {
      attributes.removeIf(
          attribute -> attribute.getType().equals(GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA));
      attributes.removeIf(
          attribute -> attribute.getType().equals(GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY));

      attributes.add(Attribute.build(GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA, "not provided"));

      log.info(
          "geo_loc_name attribute not present, set '{}' as 'not provided' for sample {}",
          GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA,
          accession);
    }

    // Handle collection date
    final Optional<Attribute> collectionDateOptional =
        attributes.stream().filter(attr -> COLLECTION_DATE.equals(attr.getType())).findFirst();

    if (collectionDateOptional.isPresent()) {
      Attribute collectionDate = collectionDateOptional.get();
      String value = collectionDate.getValue();

      attributes.add(
          Attribute.build(
              COLLECTION_DATE_WITHOUT_UNDERSCORE,
              value != null ? value : "not provided",
              collectionDate.getTag(),
              Collections.emptyList(),
              null));

      log.info(
          "Processed collection_date for sample {}: {}",
          accession,
          value != null ? value : "not provided");
    } else {
      attributes.add(Attribute.build(COLLECTION_DATE_WITHOUT_UNDERSCORE, "not provided"));

      log.info(
          "collection_date attribute not present, set as 'not provided' for sample {}", accession);
    }

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

  public void updateSamnSampleGeographicLocationFromFile() {
    final Pattern pattern = Pattern.compile("(SAMN|SAMD)\\d+");
    final Set<String> samnAccessions = new HashSet<>();

    try (BufferedReader reader =
        new BufferedReader(new FileReader("C:\\Users\\dgupta\\samples_2.list"))) {
      String line;
      while ((line = reader.readLine()) != null) {
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
          samnAccessions.add(matcher.group());
        }
      }
    } catch (IOException e) {
      log.error("Error reading sample list file", e);
      throw new RuntimeException(e);
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
