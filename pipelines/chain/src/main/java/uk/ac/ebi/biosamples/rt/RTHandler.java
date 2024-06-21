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
package uk.ac.ebi.biosamples.rt;

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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;

@Component
public class RTHandler {
  private static final Logger log = LoggerFactory.getLogger(RTHandler.class);
  private static final String WEBIN_161 = "Webin-161";
  private static final String BIOSAMPLE_SYNTHETIC_DATA = "self.BiosampleSyntheticData";
  private static final String GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA =
      "geographic location (country and/or sea)";
  private static final String GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY =
      "geographic location (region and locality)";
  private static final String ENA_CHECKLIST = "ENA-CHECKLIST";
  private final BioSamplesClient bioSamplesWebinClient;
  private final BioSamplesClient bioSamplesAapClient;
  private final PipelinesProperties pipelinesProperties;
  private final MongoSampleRepository mongoSampleRepository;
  private final RestTemplate restTemplate;

  public RTHandler(
      @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesWebinClient,
      @Qualifier("AAPCLIENT") final BioSamplesClient bioSamplesAapClient,
      final PipelinesProperties pipelinesProperties,
      final MongoSampleRepository mongoSampleRepository) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.bioSamplesAapClient = bioSamplesAapClient;
    this.pipelinesProperties = pipelinesProperties;
    this.mongoSampleRepository = mongoSampleRepository;
    this.restTemplate = new RestTemplate();
  }

  public void readFileAndProcessSamples() {
    final String filePath = "C:\\Users\\dgupta\\ncbi_samples.list";

    try {
      final BufferedReader reader = new BufferedReader(new FileReader(filePath));

      String line;

      while ((line = reader.readLine()) != null) {
        checkSampleExistance(line);
      }

      reader.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  private void checkSampleExistance(final String accession) {
    String url = "https://www.ebi.ac.uk/biosamples/samples/" + accession;

    try {
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

      // Check if the response status is not 2XX
      if (response.getStatusCode().isError()) {
        throw new HttpClientErrorException(response.getStatusCode());
      }

      // If successful, you can handle the response here
      log.info("Response Code: " + response.getStatusCode());
    } catch (HttpClientErrorException e) {
      // Handle exceptions caused by non-2XX responses
      log.error("HTTP Error: " + e.getStatusCode() + " - " + e.getStatusText());
    } catch (Exception e) {
      // Handle other exceptions
      log.error("An error occurred: " + e.getMessage());
    }
  }

  public void readFileAndFixAuthOfSamples() {
    final String filePath = "C:\\Users\\dgupta\\biosamples_1.list";
    final List<String> curationDomainBlankList = new ArrayList<>();

    curationDomainBlankList.add("");

    try {
      final BufferedReader reader = new BufferedReader(new FileReader(filePath));

      String line;

      while ((line = reader.readLine()) != null) {
        handleSampleAuth(line);
      }

      reader.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  private void handleSampleAuth(final String accession) {
    log.info("Handling " + accession);

    final Optional<MongoSample> mongoSampleOptional = mongoSampleRepository.findById(accession);

    if (mongoSampleOptional.isPresent()) {
      final MongoSample mongoSample = mongoSampleOptional.get();

      log.info("Webin ID is " + mongoSample.getWebinSubmissionAccountId());
      log.info("Domain is " + mongoSample.getDomain());

      final String domainOfSampleInDev = fetchDevSampleAndCheckAuth(accession).getDomain();

      if (domainOfSampleInDev != null) {
        log.info("Sample in dev, the domain is " + domainOfSampleInDev);

        mongoSample.setWebinSubmissionAccountId(null);
        mongoSample.setDomain(domainOfSampleInDev);

        log.info(
            "Updating sample "
                + mongoSample.getAccession()
                + " to domain "
                + mongoSample.getDomain());
        mongoSampleRepository.save(mongoSample);
      }
    }
  }

  public Sample fetchDevSampleAndCheckAuth(String accession) {
    final String url = "https://wwwdev.ebi.ac.uk/biosamples/samples/" + accession;

    return restTemplate.getForObject(url, Sample.class);
  }

  private void processSample(final String accession, final List<String> curationDomainList) {
    log.info("Processing Sample: " + accession);

    Optional<EntityModel<Sample>> optionalSampleEntityModel =
        bioSamplesAapClient.fetchSampleResource(accession, Optional.of(curationDomainList));

    if (optionalSampleEntityModel.isEmpty()) {
      optionalSampleEntityModel =
          bioSamplesWebinClient.fetchSampleResource(accession, Optional.of(curationDomainList));
    }

    /*if (optionalSampleEntityModel.isPresent()) {
        final Sample sample = optionalSampleEntityModel.get().getContent();
        assert sample != null;
        final String sampleDomain = sample.getDomain();
        final String sampleWebinId = sample.getWebinSubmissionAccountId();

        if (sampleDomain != null) {
          log.info("Sample authority is correct for " + accession + " no updates required");
        } else if (!sampleWebinId.equals(pipelinesProperties.getProxyWebinId())) {
          log.info("Sample authority is correct for " + accession + " no updates required");
        } else {
          if (sample.getAttributes().stream()
              .anyMatch(attribute -> attribute.getType().equalsIgnoreCase(ENA_CHECKLIST))) {
            log.info("Sample not BioSample authority " + accession + " no updates required");
          }

          if (sample.getSubmittedVia() == SubmittedViaType.PIPELINE_IMPORT
              || sample.getSubmittedVia() == SubmittedViaType.WEBIN_SERVICES) {
            log.info("Sample is an imported sample " + accession + " no updates required");
          }

          log.info("Sample authority is incorrect for " + accession + " setting to " + WEBIN_161);

          final Sample updatedSample =
              Sample.Builder.fromSample(sample)
                  .withWebinSubmissionAccountId(WEBIN_161)
                  .withNoDomain()
                  .build();
          final EntityModel<Sample> sampleEntityModel =
              bioSamplesWebinClient.persistSampleResource(updatedSample);

          if (Objects.requireNonNull(sampleEntityModel.getContent())
              .getWebinSubmissionAccountId()
              .equals(WEBIN_161)) {
            log.info("Sample " + accession + " updated");
          } else {
            log.info("Sample " + accession + " failed to be updated");
          }
        }
      } else {
        log.info("Sample not found " + accession);
      }
    }*/

    final Sample sample = optionalSampleEntityModel.get().getContent();

    assert sample != null;

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
      log.info("geo_loc_name attribute present in " + accession);

      final Attribute geoLocAttribute = getLocAttributeOptional.get();
      final String getLocAttrValue = geoLocAttribute.getValue();
      final String getLocAttributeTag = geoLocAttribute.getTag();
      final String getLocAttributeUnit = geoLocAttribute.getUnit();
      final List<String> extractedCountryNameList = splitGeoLoc(getLocAttrValue);
      /*final String extractedRegionName = countryAndRegionExtractor(getLocAttrValue, 2);

      if (extractedRegionName != null && !extractedRegionName.isEmpty()) {
        attributeSet.removeIf(attribute -> attribute.getType().equals(GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY));
        attributeSet.add(
                Attribute.build(
                        GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY,
                        extractedRegionName,
                        getLocAttributeTag,
                        Collections.emptyList(),
                        getLocAttributeUnit));
      }*/

      /*attributeSet.removeIf(
      attribute -> attribute.getType().equals(GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY));*/

      if (extractedCountryNameList.get(0) != null && !extractedCountryNameList.get(0).isEmpty()) {
        attributeSet.removeIf(
            attribute -> attribute.getType().equals(GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA));
        attributeSet.add(
            Attribute.build(
                GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA,
                extractedCountryNameList.get(0),
                getLocAttributeTag,
                Collections.emptyList(),
                getLocAttributeUnit));
      } else {
        attributeSet.add(Attribute.build(GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA, "not provided"));
      }
    } else {
      log.info(
          "geo_loc_name attribute not present in "
              + accession
              + " building with not provided)value");

      attributeSet.add(Attribute.build(GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA, "not provided"));
    }

    if (collectionDateAttributeOptional.isEmpty()) {
      log.info(
          "collection_date attribute not present in "
              + accession
              + " adding new attribute with non provided value");
      attributeSet.add(Attribute.build("collection_date", "not provided"));
    } /*else {
            final Attribute collectionDateAttribute = collectionDateAttributeOptional.get();
            final String collectionDateAttributeValue = collectionDateAttribute.getValue();

            if (collectionDateAttributeValue.equals("not provided")) {
              log.info(
                  "collection_date attribute present in "
                      + accession
                      + " and already set to not provided");
            } else {
              log.info(
                  "collection_date attribute present in "
                      + accession
                      + " but not set to not provided, setting now");

              attributeSet.remove(collectionDateAttribute);
              attributeSet.add(Attribute.build("collection_date", "not provided"));
            }
      */
    final Sample updateSample =
        Sample.Builder.fromSample(sample).withAttributes(attributeSet).build();

    bioSamplesAapClient.persistSampleResource(updateSample);
  }

  public void samnSampleGeographicLocationAttributeUpdate() {
    final List<String> sampleStrings =
        List.of(
            "SAMN31710087, SAMN31710088, SAMN31710089, SAMN31710090, SAMN31710091, SAMN31710092, SAMN31710093, SAMN31710094, SAMN31710095, SAMN31710096, SAMN31710097, SAMN31710098, SAMN31710099, SAMN31710100, SAMN31710101, SAMN31710102, SAMN31710103, SAMN31710104, SAMN31710105, SAMN31710106, SAMN31710107, SAMN31710108, SAMN31710109, SAMN31710110, SAMN31710111, SAMN31710112, SAMN31710113, SAMN31710114, SAMN31710115, SAMN31710116, SAMN31710117, SAMN31710118, SAMN31710119, SAMN31710120, SAMN31710121, SAMN31710122, SAMN31710123, SAMN31710124, SAMN31710125, SAMN31710126, SAMN31710127, SAMN31710128, SAMN31710129, SAMN31710130, SAMN31710131, SAMN31710132, SAMN31710133, SAMN31710134, SAMN31710135, SAMN31710136, SAMN31710137, SAMN31710138, SAMN31710139, SAMN31710140, SAMN31710141, SAMN31710142, SAMN31710143, SAMN31710144, SAMN31710145, SAMN31710146\n");

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

  private String countryAndRegionExtractor(final String getLocAttrValue, final int i) {
    /*
    final Pattern pattern = Pattern.compile("^[a-zA-Z]+:\\s*(.+)$");
    final Matcher matcher = pattern.matcher(getLocAttrValue);

    if (matcher.find()) {
      final String countryOrRegionName = matcher.group(1);
      System.out.println("Extracted country/ region is: " + countryOrRegionName);

      return countryOrRegionName;
    } else {
      System.out.println("Country/ Region not found in the input string.");

      return null;
    }*/

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
      // Handle case where ':' is not found
      parts.add(input.trim());
      parts.add(""); // or handle as needed, e.g., throw an exception
    }

    return parts;
  }
}
