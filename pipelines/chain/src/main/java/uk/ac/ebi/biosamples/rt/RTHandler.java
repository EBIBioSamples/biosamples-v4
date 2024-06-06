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
      final String extractedCountryName = countryAndRegionExtractor(getLocAttrValue, 1);
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

      if (extractedCountryName != null && !extractedCountryName.isEmpty()) {
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
            "SAMN37299522 \n"
                + "SAMN37299523\n"
                + "SAMN37299524\n"
                + "SAMN37299525\n"
                + "SAMN37299526\n"
                + "SAMN37299527\n"
                + "SAMN37299528\n"
                + "SAMN37299529\n"
                + "SAMN37299530\n"
                + "SAMN37299531\n"
                + "SAMN37299532\n"
                + "SAMN37299533\n"
                + "SAMN37299534\n"
                + "SAMN37299535\n"
                + "SAMN37299536\n"
                + "SAMN37299537");

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
}
