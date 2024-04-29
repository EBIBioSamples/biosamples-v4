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
package uk.ac.ebi.biosamples.misc;

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

  public void parseIdentifiersFromFileAndCheckSampleExistence() {
    final String filePath = "C:\\Users\\dgupta\\ncbi_samples.list";

    try {
      final BufferedReader reader = new BufferedReader(new FileReader(filePath));

      String line;

      while ((line = reader.readLine()) != null) {
        checkExistence(line);
      }

      reader.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  private void checkExistence(final String accession) {
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

  public void parseIdentifiersFromFileAndFixAuth() {
    final String filePath = "C:\\Users\\dgupta\\biosamples_1.list";
    final List<String> curationDomainBlankList = new ArrayList<>();

    curationDomainBlankList.add("");

    try {
      final BufferedReader reader = new BufferedReader(new FileReader(filePath));

      String line;

      while ((line = reader.readLine()) != null) {
        processSampleAlterAuth(line);
      }

      reader.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  private void processSampleAlterAuth(final String accession) {
    log.info("Handling " + accession);

    final Optional<MongoSample> mongoSampleOptional = mongoSampleRepository.findById(accession);

    if (mongoSampleOptional.isPresent()) {
      final MongoSample mongoSample = mongoSampleOptional.get();

      log.info("Webin ID is " + mongoSample.getWebinSubmissionAccountId());
      log.info("Domain is " + mongoSample.getDomain());

      final String domainOfSampleInDev =
          getSampleByAccessionFromDevToCheckDomain(accession).getDomain();

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

  public Sample getSampleByAccessionFromDevToCheckDomain(String accession) {
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

      if (collectionDateAttributeOptional.isEmpty()) {
        log.info(
            "collection_date attribute not present in "
                + accession
                + " adding new attribute with non provided value");
        attributeSet.add(Attribute.build("collection_date", "not provided"));
      }

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
      }
      final Sample updateSample =
          Sample.Builder.fromSample(sample).withAttributes(attributeSet).build();

      bioSamplesAapClient.persistSampleResource(updateSample);
    } else {
      log.info("geo_loc_name attribute not present in " + accession);
    }
  }

  public void samnSampleGeographicLocationAttributeUpdate() {
    final List<String> sampleStrings =
        List.of(
            "SAMN29388702 \n"
                + "SAMN29388703 \n"
                + "SAMN29388706 \n"
                + "SAMN29388708 \n"
                + "SAMN29388819 \n"
                + "SAMN29388820 \n"
                + "SAMN29388821 \n"
                + "SAMN29388822 \n"
                + "SAMN29388823 \n"
                + "SAMN29388824 \n"
                + "SAMN29388825 \n"
                + "SAMN29388826 \n"
                + "SAMN29388827 \n"
                + "SAMN29388828 \n"
                + "SAMN29388829 \n"
                + "SAMN29388831 \n"
                + "SAMN29388832 \n"
                + "SAMN29388833 \n"
                + "SAMN29388834 \n"
                + "SAMN29388835 \n"
                + "SAMN29388836 \n"
                + "SAMN29388837 \n"
                + "SAMN29388838 \n"
                + "SAMN29388839 \n"
                + "SAMN29388840 \n"
                + "SAMN29388841 \n"
                + "SAMN29388842 \n"
                + "SAMN29388843 \n"
                + "SAMN29388716 \n"
                + "SAMN29388717 \n"
                + "SAMN29388718 \n"
                + "SAMN29388719 \n"
                + "SAMN29388720 \n"
                + "SAMN29388721 \n"
                + "SAMN29388722 \n"
                + "SAMN29388723 \n"
                + "SAMN29388724 \n"
                + "SAMN29388725 \n"
                + "SAMN29388726 \n"
                + "SAMN29388727 \n"
                + "SAMN29388728 \n"
                + "SAMN29388729 \n"
                + "SAMN29388730 \n"
                + "SAMN29388731 \n"
                + "SAMN29388732 \n"
                + "SAMN29388733 \n"
                + "SAMN29388734 \n"
                + "SAMN29388735 \n"
                + "SAMN29388736 \n"
                + "SAMN29388737 \n"
                + "SAMN29388738 \n"
                + "SAMN29388739 \n"
                + "SAMN29388740 \n"
                + "SAMN29388741 \n"
                + "SAMN29388742 \n"
                + "SAMN29388743 \n"
                + "SAMN29388744 \n"
                + "SAMN29388745 \n"
                + "SAMN29388746 \n"
                + "SAMN29388747 \n"
                + "SAMN29388748 \n"
                + "SAMN29388749 \n"
                + "SAMN29388750 \n"
                + "SAMN29388751 \n"
                + "SAMN29388752 \n"
                + "SAMN29388753 \n"
                + "SAMN29388754 \n"
                + "SAMN29388755 \n"
                + "SAMN30432961 \n"
                + "SAMN30432962 \n"
                + "SAMN30432963 \n"
                + "SAMN30432964 \n"
                + "SAMN30432965 \n"
                + "SAMEA7066375 \n"
                + "SAMEA7066376 \n"
                + "SAMN30432966 \n"
                + "SAMN30432967 \n"
                + "SAMN30432968 \n"
                + "SAMN30432969 \n"
                + "SAMN30432970 \n"
                + "SAMN30432971 \n"
                + "SAMEA7066377 \n"
                + "SAMN30432972 \n"
                + "SAMN29388772 \n"
                + "SAMN29388773 \n"
                + "SAMN29388774 \n"
                + "SAMN29388775 \n"
                + "SAMN29388781 \n"
                + "SAMN29388776 \n"
                + "SAMN29388767 \n"
                + "SAMN29388777 \n"
                + "SAMN29388779 \n"
                + "SAMN29388780\n"
                + "SAMN29388785\n"
                + "SAMN29388786\n"
                + "SAMN29388787\n"
                + "SAMN29388788\n"
                + "SAMN29388793\n"
                + "SAMN29388794\n"
                + "SAMN29388800\n"
                + "SAMN29388802\n"
                + "SAMN29388809\n"
                + "SAMN29388810\n"
                + "SAMN29388811\n"
                + "SAMN29388812\n"
                + "SAMN29388813\n"
                + "SAMN29388814\n"
                + "SAMN29388815\n"
                + "SAMN29388816\n"
                + "SAMN29388817\n"
                + "SAMN29388818");

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
