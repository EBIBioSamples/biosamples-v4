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
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

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

  public RTHandler(
      @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesWebinClient,
      @Qualifier("AAPCLIENT") final BioSamplesClient bioSamplesAapClient,
      final PipelinesProperties pipelinesProperties) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.bioSamplesAapClient = bioSamplesAapClient;
    this.pipelinesProperties = pipelinesProperties;
  }

  public void parseIdentifiersFromFileAndFixAuth() {
    final String filePath = "C:\\Users\\dgupta\\biosamples.list";
    final List<String> curationDomainBlankList = new ArrayList<>();

    curationDomainBlankList.add("");

    try {
      final BufferedReader reader = new BufferedReader(new FileReader(filePath));

      String line;

      while ((line = reader.readLine()) != null) {
        processSample(line, curationDomainBlankList);
      }

      reader.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
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

      attributeSet.removeIf(
          attribute -> attribute.getType().equals(GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY));

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

      bioSamplesWebinClient.persistSampleResource(updateSample);
    } else {
      log.info("geo_loc_name attribute not present in " + accession);
    }
  }

  public void samnSampleGeographicLocationAttributeUpdate() {
    final List<String> sampleStrings =
        List.of(
            "existing sample accession SAMN15298114 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298115 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298083 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298102 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298097 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298095 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298111 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298110 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298088 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298098 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298101 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298107 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298106 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231944 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231957 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231940 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231961 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231955 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231937 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231953 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231936 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231947 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231954 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298116 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298117 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298118 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298132 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298091 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298089 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298090 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298131 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298086 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298127 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298128 does not have a valid geographic location\n"
                + "existing sample accession SAMN15298125 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231891 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231892 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231893 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231890 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231918 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231922 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231897 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231900 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231926 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231919 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231887 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231888 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231885 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231881 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231880 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231882 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231884 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231878 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231883 does not have a valid geographic location\n"
                + "existing sample accession SAMN15231889 does not have a valid geographic location");

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
