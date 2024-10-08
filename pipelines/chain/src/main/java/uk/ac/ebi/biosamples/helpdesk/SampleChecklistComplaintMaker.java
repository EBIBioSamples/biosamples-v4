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
package uk.ac.ebi.biosamples.helpdesk;

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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

@Component
public class SampleChecklistComplaintMaker {
  private static final Logger log = LoggerFactory.getLogger(SampleChecklistComplaintMaker.class);
  private static final String GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA =
      "geographic location (country and/or sea)";
  private static final String GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY =
      "geographic location (region and locality)";
  public static final String NCBI_MIRRORING_WEBIN_ID = "Webin-842";
  private final BioSamplesClient bioSamplesWebinClient;
  private final BioSamplesClient bioSamplesAapClient;
  private final PipelinesProperties pipelinesProperties;

  public SampleChecklistComplaintMaker(
      @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesWebinClient,
      @Qualifier("AAPCLIENT") final BioSamplesClient bioSamplesAapClient,
      final PipelinesProperties pipelinesProperties) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.bioSamplesAapClient = bioSamplesAapClient;
    this.pipelinesProperties = pipelinesProperties;
  }

  private void processSample(final String accession, final List<String> curationDomainList) {
    log.info("Processing Sample: " + accession);

    Optional<EntityModel<Sample>> optionalSampleEntityModel =
        bioSamplesAapClient.fetchSampleResource(accession, Optional.of(curationDomainList));

    if (optionalSampleEntityModel.isEmpty()) {
      optionalSampleEntityModel =
          bioSamplesWebinClient.fetchSampleResource(accession, Optional.of(curationDomainList));
    }

    if (optionalSampleEntityModel.isPresent()) {
      handleGeoLocAndCollectionDate(optionalSampleEntityModel);
    } else {
      log.info("Sample not found: " + accession);
    }
  }

  private void handleGeoLocAndCollectionDate(
      Optional<EntityModel<Sample>> optionalSampleEntityModel) {
    final Sample sample = optionalSampleEntityModel.get().getContent();

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
      final String extractedCountryName = countryAndRegionExtractor(getLocAttrValue);
      final String extractedRegionName = countryAndRegionExtractor(getLocAttrValue);

      if (!extractedRegionName.isEmpty()) {
        log.info("setting " + GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY + " for " + accession);

        attributeSet.removeIf(
            attribute -> attribute.getType().equals(GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY));
        attributeSet.add(
            Attribute.build(
                GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY,
                extractedRegionName,
                getLocAttributeTag,
                Collections.emptyList(),
                getLocAttributeUnit));
      } else {
        attributeSet.add(Attribute.build(GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY, "not provided"));
      }

      if (!extractedCountryName.isEmpty()) {
        log.info("setting " + GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA + " for " + accession);

        attributeSet.removeIf(
            attribute -> attribute.getType().equals(GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA));
        attributeSet.add(
            Attribute.build(
                GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA,
                extractedCountryName,
                getLocAttributeTag,
                Collections.emptyList(),
                getLocAttributeUnit));
      } else {
        attributeSet.add(Attribute.build(GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA, "not provided"));
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
      bioSamplesAapClient.persistSampleResource(updateSample);
    } catch (final HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
        log.info(
            "Failed to persist using AAP client - UNAUTHORIZED, "
                + "attempting persist using WEBIN client "
                + accession);

        if (sample.getWebinSubmissionAccountId().equals(NCBI_MIRRORING_WEBIN_ID)
            || sample.getWebinSubmissionAccountId().equals(pipelinesProperties.getProxyWebinId()))
          bioSamplesWebinClient.persistSampleResource(sample);

        log.info("Persisted using WEBIN client " + accession);
      }
    }
  }

  public void samnSampleGeographicLocationAttributeUpdate() {
    final List<String> sampleStrings =
        List.of(
            "SAMN31710087, SAMN31710088, SAMN31710089, SAMN31710090, SAMN31710091, SAMN31710092, SAMN31710093, SAMN31710094, SAMN31710095, SAMN31710096, SAMN31710097, SAMN31710098, SAMN31710099, SAMN31710100, SAMN31710101, SAMN31710102, SAMN31710103, SAMN31710104, SAMN31710105, SAMN31710106, SAMN31710107, SAMN31710108, SAMN31710109, SAMN31710110, SAMN31710111, SAMN31710112, SAMN31710113, SAMN31710114, SAMN31710115, SAMN31710116, SAMN31710117, SAMN31710118, SAMN31710119, SAMN31710120, SAMN31710121, SAMN31710122, SAMN31710123, SAMN31710124, SAMN31710125, SAMN31710126, SAMN31710127, SAMN31710128, SAMN31710129, SAMN31710130, SAMN31710131, SAMN31710132, SAMN31710133, SAMN31710134, SAMN31710135, SAMN31710136, SAMN31710137, SAMN31710138, SAMN31710139, SAMN31710140, SAMN31710141, SAMN31710142, SAMN31710143, SAMN31710144, SAMN31710145, SAMN31710146");

    final Pattern pattern = Pattern.compile("SAMN\\d+");
    final Set<String> samnAccessions = new HashSet<>();

    for (final String sampleString : sampleStrings) {
      final Matcher matcher = pattern.matcher(sampleString);
      while (matcher.find()) {
        samnAccessions.add(matcher.group());
      }
    }

    for (final String accession : samnAccessions) {
      processSample(accession, Collections.singletonList(""));
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

      processSample(accession, Collections.singletonList(""));
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
