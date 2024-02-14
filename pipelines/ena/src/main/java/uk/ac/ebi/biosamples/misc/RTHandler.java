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
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;

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

    if (optionalSampleEntityModel.isPresent()) {
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
  }

  /*final Sample sample = optionalSampleEntityModel.get().getContent();

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
      final String extractedRegionName = countryAndRegionExtractor(getLocAttrValue, 2);

      attributeSet.add(
          Attribute.build(
              GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY,
              extractedRegionName,
              getLocAttributeTag,
              Collections.emptyList(),
              getLocAttributeUnit));

      attributeSet.add(
          Attribute.build(
              GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA,
              extractedCountryName,
              getLocAttributeTag,
              Collections.emptyList(),
              getLocAttributeUnit));

      final Sample updateSample =
          Sample.Builder.fromSample(sample).withAttributes(attributeSet).build();

      bioSamplesWebinClient.persistSampleResource(updateSample);
    } else {
      log.info("geo_loc_name attribute not present in " + accession);
    }
  }*/

  public void samnSampleGeographicLocationAttributeUpdate() {
    final List<String> sampleStrings =
        List.of(
            "SAMN38658779\n"
                + "SAMN38658780\n"
                + "SAMN38658781\n"
                + "SAMN38658782\n"
                + "SAMN38658783\n"
                + "SAMN38658784\n"
                + "SAMN38658785\n"
                + "SAMN38658786\n"
                + "SAMN38658787\n"
                + "SAMN38658788\n"
                + "SAMN38658789\n"
                + "SAMN38658790\n"
                + "SAMN38658791\n"
                + "SAMN38658792\n"
                + "SAMN38658793\n"
                + "SAMN38658794\n"
                + "SAMN38658795\n"
                + "SAMN38658796\n"
                + "SAMN38658797\n"
                + "SAMN38658798\n"
                + "SAMN38658799\n"
                + "SAMN38658800\n"
                + "SAMN38658801\n"
                + "SAMN38658802\n"
                + "SAMN38658803\n"
                + "SAMN38658804\n"
                + "SAMN38658805\n"
                + "SAMN38658806\n"
                + "SAMN38658807\n"
                + "SAMN38658808\n"
                + "SAMN38658809\n"
                + "SAMN38658810\n"
                + "SAMN38658811\n"
                + "SAMN38658812\n"
                + "SAMN38658813\n"
                + "SAMN38658814\n"
                + "SAMN38658815\n"
                + "SAMN38658816\n"
                + "SAMN38658817\n"
                + "SAMN38658818\n"
                + "SAMN38658819\n"
                + "SAMN38658820\n"
                + "SAMN38658821\n"
                + "SAMN38658822\n"
                + "SAMN38658823\n"
                + "SAMN38658824\n"
                + "SAMN38658825\n"
                + "SAMN38658826\n"
                + "SAMN38658827\n"
                + "SAMN38658828\n"
                + "SAMN38658829\n"
                + "SAMN38658830\n"
                + "SAMN38658831\n"
                + "SAMN38658832\n"
                + "SAMN38658833\n"
                + "SAMN38658834\n"
                + "SAMN38658835\n"
                + "SAMN38658836\n"
                + "SAMN38658837\n"
                + "SAMN38658838\n"
                + "SAMN38658839\n"
                + "SAMN38658840\n"
                + "SAMN38658841\n"
                + "SAMN38658842\n"
                + "SAMN38658843\n"
                + "SAMN38658844\n"
                + "SAMN38658845\n"
                + "SAMN38658846\n"
                + "SAMN38658847\n"
                + "SAMN38658848\n"
                + "SAMN38658849\n"
                + "SAMN38658850\n"
                + "SAMN38658851\n"
                + "SAMN38658852\n"
                + "SAMN38658853\n"
                + "SAMN38658854\n"
                + "SAMN38658855\n"
                + "SAMN38658856\n"
                + "SAMN38658857\n"
                + "SAMN38658858\n"
                + "SAMN38658859\n"
                + "SAMN38658860\n"
                + "SAMN38658861\n"
                + "SAMN38658862\n"
                + "SAMN38658863\n"
                + "SAMN38658864\n"
                + "SAMN38658865\n"
                + "SAMN38658866\n"
                + "SAMN38658867\n"
                + "SAMN38658868\n"
                + "SAMN38658869\n"
                + "SAMN38658870\n"
                + "SAMN38658871\n"
                + "SAMN38658872\n"
                + "SAMN38658873\n"
                + "SAMN38658874\n"
                + "SAMN38658875\n"
                + "SAMN38658876\n"
                + "SAMN38658877\n"
                + "SAMN38658878\n"
                + "SAMN38658879\n"
                + "SAMN38658880\n"
                + "SAMN38658881\n"
                + "SAMN38658882\n"
                + "SAMN38658883\n"
                + "SAMN38658884\n"
                + "SAMN38658885\n"
                + "SAMN38658886\n"
                + "SAMN38658887\n"
                + "SAMN38658888\n"
                + "SAMN38658889\n"
                + "SAMN38658890\n"
                + "SAMN38658891\n"
                + "SAMN38658892\n"
                + "SAMN38658893\n"
                + "SAMN38658894\n"
                + "SAMN38658895\n"
                + "SAMN38658896\n"
                + "SAMN38658897\n"
                + "SAMN38658898");

    final Pattern pattern = Pattern.compile("SAMN\\d+");
    final List<String> samnAccessions = new ArrayList<>();

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
    final Pattern pattern = Pattern.compile("^([a-zA-Z]+):\\s*([a-zA-Z]+)$");
    final Matcher matcher = pattern.matcher(getLocAttrValue);

    if (matcher.find()) {
      final String countryOrRegionName = matcher.group(i);
      System.out.println("Extracted country/ region is: " + countryOrRegionName);

      return countryOrRegionName;
    } else {
      System.out.println("Country/ Region not found in the input string.");

      return null;
    }
  }
}
