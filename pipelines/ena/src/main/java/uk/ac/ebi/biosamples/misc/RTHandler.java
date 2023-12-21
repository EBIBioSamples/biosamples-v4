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

import org.springframework.stereotype.Component;

@Component
public class RTHandler {
  /*private static final Logger log = LoggerFactory.getLogger(RTHandler.class);
  public static final String WEBIN_58957 = "Webin-XXXXX";
  public static final String BIOSAMPLE_SYNTHETIC_DATA = "self.BiosampleSyntheticData";
  public static final String GEOGRAPHIC_LOCATION_COUNTRY_AND_OR_SEA =
      "geographic location (country and/or sea)";
  public static final String GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY =
      "geographic location (region and locality)";
  private final BioSamplesClient bioSamplesWebinClient;
  private final BioSamplesClient bioSamplesAapClient;

  public RTHandler(
      @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesWebinClient,
      @Qualifier("AAPCLIENT") final BioSamplesClient bioSamplesAapClient) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.bioSamplesAapClient = bioSamplesAapClient;
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

  private void processSample(final String accession, final List<String> curationDomainBlankList) {
    log.info("Processing Sample: " + accession);

    Optional<EntityModel<Sample>> optionalSampleEntityModel =
        bioSamplesAapClient.fetchSampleResource(accession, Optional.of(curationDomainBlankList));

    if (optionalSampleEntityModel.isEmpty()) {
      optionalSampleEntityModel =
          bioSamplesWebinClient.fetchSampleResource(
              accession, Optional.of(curationDomainBlankList));
    }

    if (optionalSampleEntityModel.isPresent()) {
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
        final String extractedRegionName = countryAndRegionExtractor(getLocAttrValue, 2);

        attributeSet.add(
            Attribute.build(
                GEOGRAPHIC_LOCATION_REGION_AND_LOCALITY,
                extractedRegionName,
                getLocAttributeTag,
                Collections.emptyList(),
                getLocAttributeUnit));

        final Sample updateSample =
            Sample.Builder.fromSample(sample).withAttributes(attributeSet).build();

        bioSamplesWebinClient.persistSampleResource(updateSample);
      } else {
        log.info("geo_loc_name attribute not present in " + accession);
      }

      */
  /* final Sample sample = optionalSampleEntityModel.get().getContent();
  final String sampleDomain = sample.getDomain();

  if (sampleDomain != null && sampleDomain.equals(BIOSAMPLE_SYNTHETIC_DATA)) {
    log.info(
        "Sample authority is incorrect for " + accession + " updating to correct authority");

    final Sample updatedSample =
        Sample.Builder.fromSample(sample)
            .withWebinSubmissionAccountId(WEBIN_58957)
            .withNoDomain()
            .build();
    final EntityModel<Sample> sampleEntityModel =
        bioSamplesWebinClient.persistSampleResource(updatedSample);

    if (sampleEntityModel.getContent().getWebinSubmissionAccountId().equals(WEBIN_58957)) {
      log.info("Sample " + accession + " updated");
    } else {
      log.info("Sample " + accession + " failed to be updated");
    }
  } else {
    log.info("Sample authority is correct " + accession);
  }*/
  /*
    }
  }

  public void samnSampleGeographicLocationAttributeUpdate() {
    final List<String> curationDomainBlankList = new ArrayList<>();

    curationDomainBlankList.add("");

    final List<String> sampleStrings =
        List.of(
            "https://www.ebi.ac.uk/ena/browser/view/SAMN37286570\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286571\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286572\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286573\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286574\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286575\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286576\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286577\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286578\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286579\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286580\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286581\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286582\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286583\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286584\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286585\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286586\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286587\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286588\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286589\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286590\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286591\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286592\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286593\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286594\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286595\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286596\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286597\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286598\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286599\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286600\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286601\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286602\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286603\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286604\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286605\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286606\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286607\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286608\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286609\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286610\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286611\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286612\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286613\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286614\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286615\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286616\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286617\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286618\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286619\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286620\n"
                + "https://www.ebi.ac.uk/ena/browser/view/SAMN37286621");

    final Pattern pattern = Pattern.compile("SAMN\\d+");

    final List<String> samnAccessions = new ArrayList<>();

    for (final String sampleString : sampleStrings) {
      final Matcher matcher = pattern.matcher(sampleString);

      while (matcher.find()) {
        samnAccessions.add(matcher.group());
      }
    }

    for (final String accession : samnAccessions) {
      processSample(accession, curationDomainBlankList);
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
  }*/
}
