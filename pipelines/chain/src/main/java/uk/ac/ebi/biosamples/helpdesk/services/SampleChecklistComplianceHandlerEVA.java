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
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

@Component
public class SampleChecklistComplianceHandlerEVA {
  private static final Logger log =
      LoggerFactory.getLogger(SampleChecklistComplianceHandlerEVA.class);
  private static final String GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA =
      "geographic location (country and/or sea)";
  private static final String GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY =
      "geographic location (region and locality)";
  public static final String NCBI_MIRRORING_WEBIN_ID = "Webin-842";
  private final BioSamplesClient bioSamplesWebinClient;
  private final PipelinesProperties pipelinesProperties;

  public SampleChecklistComplianceHandlerEVA(
      @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesWebinClient,
      final PipelinesProperties pipelinesProperties) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.pipelinesProperties = pipelinesProperties;
  }

  private void processSample(final String accession) {
    log.info("Processing Sample: " + accession);

    final Optional<EntityModel<Sample>> optionalSampleEntityModel =
        bioSamplesWebinClient.fetchSampleResource(accession, false);

    if (optionalSampleEntityModel.isPresent()) {
      handleGeographicLocationAndCollectionDate(optionalSampleEntityModel);
    } else {
      log.info("Sample not found: " + accession);
    }
  }

  private void handleGeographicLocationAndCollectionDate(
      Optional<EntityModel<Sample>> optionalSampleEntityModel) {
    final Sample sample = optionalSampleEntityModel.orElseGet(null).getContent();

    if (sample == null) {
      return;
    }

    final String accession = sample.getAccession();
    final Set<Attribute> attributeSet = sample.getAttributes();

    final Optional<Attribute> getLocAttributeOptional =
        attributeSet.stream()
            .filter(attribute -> attribute.getType().equals("geo_loc_name"))
            .findFirst();
    final Optional<Attribute> collectionDateAttributeOptional =
        attributeSet.stream()
            .filter(attribute -> attribute.getType().equals("collection_date"))
            .findFirst();

    if (getLocAttributeOptional.isPresent()) {
      log.info("geo_loc_name attribute present in: " + accession);

      final Attribute geoLocAttribute = getLocAttributeOptional.get();
      final String getLocAttrValue = geoLocAttribute.getValue();
      final String getLocAttributeTag = geoLocAttribute.getTag();
      final String getLocAttributeUnit = geoLocAttribute.getUnit();
      // final List<String> splittedGeoLoc = splitGeoLoc(getLocAttrValue);
      final String geoLocValue = countryAndRegionExtractor(getLocAttrValue);

      if (!geoLocValue.isEmpty()) {
        log.info(
            "Setting "
                + GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA
                + " and "
                + GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY
                + " for "
                + accession);

        attributeSet.removeIf(
            attribute -> attribute.getType().equals(GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA));
        attributeSet.removeIf(
            attribute -> attribute.getType().equals(GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY));
        attributeSet.add(
            Attribute.build(
                GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA,
                "South Korea",
                getLocAttributeTag,
                Collections.emptyList(),
                getLocAttributeUnit));
        attributeSet.add(
            Attribute.build(
                GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY,
                "South Korea",
                getLocAttributeTag,
                Collections.emptyList(),
                getLocAttributeUnit));
      } else {
        attributeSet.add(Attribute.build(GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA, "not provided"));
        attributeSet.add(Attribute.build(GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY, "not provided"));
      }
    } else {
      log.info(
          "geo_loc_name attribute not present in: "
              + accession
              + " building with not provided value");
      attributeSet.add(Attribute.build(GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA, "not provided"));
    }

    if (collectionDateAttributeOptional.isEmpty()) {
      log.info(
          "collection_date attribute not present in: "
              + accession
              + " adding new attribute with not provided value");
      attributeSet.add(Attribute.build("collection_date", "not provided"));
    } else {
      final Attribute collectionDateAttribute = collectionDateAttributeOptional.get();
      final String collectionDateAttributeValue = collectionDateAttribute.getValue();

      if (!collectionDateAttributeValue.equals("not provided")) {
        log.info(
            "collection_date attribute present in: "
                + accession
                + " but not set to not provided, setting now");
        attributeSet.remove(collectionDateAttribute);
        attributeSet.add(Attribute.build("collection_date", "not provided"));
      } else {
        log.info(
            "collection_date attribute present in: "
                + accession
                + " and set to not provided, no action required");
      }
    }

    final Sample updateSample =
        Sample.Builder.fromSample(sample).withAttributes(attributeSet).build();

    try {
      bioSamplesWebinClient.persistSampleResource(updateSample);

      log.info("Persisted using WEBIN client " + accession);
    } catch (final Exception e) {
      log.info("Failed to persisted using WEBIN client " + accession);
    }
  }

  public void samnSampleGeographicLocationAttributeUpdateFromFile() {
    final Pattern pattern = Pattern.compile("SAMN\\d+");
    final Set<String> samnAccessions = new HashSet<>();

    try (final BufferedReader bufferedReader =
        new BufferedReader(new FileReader("C:\\Users\\dgupta\\samples.txt"))) {
      String line;

      while ((line = bufferedReader.readLine()) != null) {
        final Matcher matcher = pattern.matcher(line);

        while (matcher.find()) {
          samnAccessions.add(matcher.group());
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    for (final String accession : samnAccessions) {
      // log.info(accession);

      processSample(accession);
    }
  }

  private String countryAndRegionExtractor(final String getLocAttrValue) {
    return getLocAttrValue;
  }

  public static List<String> splitGeoLoc(final String input) {
    List<String> parts = new ArrayList<>();
    int index = input.indexOf(':');

    if (index != -1) {
      String country = input.substring(0, index).trim();
      String city = input.substring(index + 1).trim();
      parts.add(country);
      parts.add(city);
    } else {
      parts.add(input.trim());
      parts.add("");
    }

    return parts;
  }
}
