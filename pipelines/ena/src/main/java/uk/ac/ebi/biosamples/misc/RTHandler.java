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
    } else {
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
                + "but not set to not provided, setting now");

        attributeSet.remove(collectionDateAttribute);
        attributeSet.add(Attribute.build("collection_date", "not provided"));
      }
    }

    final Sample updateSample =
        Sample.Builder.fromSample(sample).withAttributes(attributeSet).build();

    bioSamplesAapClient.persistSampleResource(updateSample);
  }

  public void samnSampleGeographicLocationAttributeUpdate() {
    final List<String> sampleStrings =
        List.of(
            "SAMN39915694\n"
                + "SAMN39915695\n"
                + "SAMN39915696\n"
                + "SAMN39915697\n"
                + "SAMN39915698\n"
                + "SAMN39915699\n"
                + "SAMN39915700\n"
                + "SAMN39915701\n"
                + "SAMN39915702\n"
                + "SAMN39915703\n"
                + "SAMN04545540\n"
                + "SAMN04545541\n"
                + "SAMN04545542\n"
                + "SAMN04545543\n"
                + "SAMN04545544\n"
                + "SAMN04545545\n"
                + "SAMN04545546\n"
                + "SAMN04545547\n"
                + "SAMN04545548\n"
                + "SAMN04545549\n"
                + "SAMN04545556\n"
                + "SAMN04545558\n"
                + "SAMN04545559\n"
                + "SAMN04545560\n"
                + "SAMN04545550\n"
                + "SAMN04545551\n"
                + "SAMN04545552\n"
                + "SAMN04545553\n"
                + "SAMN04545555\n"
                + "SAMN04545557\n"
                + "SAMN04545538\n"
                + "SAMN04545539\n"
                + "SAMN04545530\n"
                + "SAMN04545531\n"
                + "SAMN04545532\n"
                + "SAMN04545533\n"
                + "SAMN04545534\n"
                + "SAMN04545535\n"
                + "SAMN05862018\n"
                + "SAMN04545537\n"
                + "SAMN04545561\n"
                + "SAMN04545562\n"
                + "SAMN04545563\n"
                + "SAMN04545564\n"
                + "SAMN04545565\n"
                + "SAMN04545566\n"
                + "SAMN04545567\n"
                + "SAMN04545568\n"
                + "SAMN04545569\n"
                + "SAMN04545570\n"
                + "SAMN04545571\n"
                + "SAMN04545573\n"
                + "SAMN04545574\n"
                + "SAMN04545575\n"
                + "SAMN04545576\n"
                + "SAMN04545577\n"
                + "SAMN04545578\n"
                + "SAMN04545579\n"
                + "SAMN07135491\n"
                + "SAMN07135492\n"
                + "SAMN07135493\n"
                + "SAMN07135494\n"
                + "SAMN07135495\n"
                + "SAMN07135496\n"
                + "SAMN07135497\n"
                + "SAMN07135498\n"
                + "SAMN07135499\n"
                + "SAMN07135500\n"
                + "SAMN15514550\n"
                + "SAMN15514551\n"
                + "SAMN15514552\n"
                + "SAMN15514553\n"
                + "SAMN15514554\n"
                + "SAMN15514555\n"
                + "SAMN15514556\n"
                + "SAMN15514557\n"
                + "SAMN15514558\n"
                + "SAMN15514477\n"
                + "SAMN15514478\n"
                + "SAMN15514479\n"
                + "SAMN15514480\n"
                + "SAMN15514481\n"
                + "SAMN15514482\n"
                + "SAMN15514483\n"
                + "SAMN15514484\n"
                + "SAMN15514485\n"
                + "SAMN15514486\n"
                + "SAMN15514487\n"
                + "SAMN15514488\n"
                + "SAMN15514489\n"
                + "SAMN15514490\n"
                + "SAMN15514491\n"
                + "SAMN15514492\n"
                + "SAMN15514493\n"
                + "SAMN15514494\n"
                + "SAMN15514495\n"
                + "SAMN15514496\n"
                + "SAMN15514497\n"
                + "SAMN15514498\n"
                + "SAMN15514499\n"
                + "SAMN15514500\n"
                + "SAMN15514501\n"
                + "SAMN15514503\n"
                + "SAMN15514504\n"
                + "SAMN15514505\n"
                + "SAMN15514507\n"
                + "SAMN15514508\n"
                + "SAMN15514509\n"
                + "SAMN15514510\n"
                + "SAMN15514511\n"
                + "SAMN15514512\n"
                + "SAMN15514513\n"
                + "SAMN15514514\n"
                + "SAMN15514515\n"
                + "SAMN15514516\n"
                + "SAMN15514517\n"
                + "SAMN15514518\n"
                + "SAMN15514519\n"
                + "SAMN15514520\n"
                + "SAMN15514521\n"
                + "SAMN15514522\n"
                + "SAMN15514523\n"
                + "SAMN15514524\n"
                + "SAMN15514525\n"
                + "SAMN15514571\n"
                + "SAMN15514572\n"
                + "SAMN15514573\n"
                + "SAMN15514574\n"
                + "SAMN15514575\n"
                + "SAMN15514576\n"
                + "SAMN15514577\n"
                + "SAMN15514578\n"
                + "SAMN15514579\n"
                + "SAMN15514526\n"
                + "SAMN15514527\n"
                + "SAMN15514528\n"
                + "SAMN15514529\n"
                + "SAMN15514530\n"
                + "SAMN15514531\n"
                + "SAMN15514534\n"
                + "SAMN15514532\n"
                + "SAMN15514533\n"
                + "SAMN15514535\n"
                + "SAMN15514580\n"
                + "SAMN15514581\n"
                + "SAMN15514582\n"
                + "SAMN15514583\n"
                + "SAMN15514584\n"
                + "SAMN15514585\n"
                + "SAMN15514586\n"
                + "SAMN15514587\n"
                + "SAMN15514588\n"
                + "SAMN15514589\n"
                + "SAMN15514590\n"
                + "SAMN15514536\n"
                + "SAMN15514538\n"
                + "SAMN15514540\n"
                + "SAMN15514541\n"
                + "SAMN15514542\n"
                + "SAMN15514543\n"
                + "SAMN15514544\n"
                + "SAMN15514545\n"
                + "SAMN15514546\n"
                + "SAMN15514547\n"
                + "SAMN15514548\n"
                + "SAMN15514549\n"
                + "SAMN15514559\n"
                + "SAMN15514560\n"
                + "SAMN15514561\n"
                + "SAMN15514562\n"
                + "SAMN15514563\n"
                + "SAMN15514564\n"
                + "SAMN15514565\n"
                + "SAMN15514566\n"
                + "SAMN15514567\n"
                + "SAMN15514568\n"
                + "SAMN15514569\n"
                + "SAMN15514570\n"
                + "SAMN13632282\n"
                + "SAMN13632279\n"
                + "SAMN13632278\n"
                + "SAMN13632281\n"
                + "SAMN13632280\n"
                + "SAMN13949653\n"
                + "SAMN13949654\n"
                + "SAMN13949655\n"
                + "SAMN13949656\n"
                + "SAMN13949657\n"
                + "SAMN13949658\n"
                + "SAMN13949659\n"
                + "SAMN13949660\n"
                + "SAMN17765866\n"
                + "SAMN17765867\n"
                + "SAMN17765868\n"
                + "SAMN17765869\n"
                + "SAMN17765870\n"
                + "SAMN17765871\n"
                + "SAMN17765872\n"
                + "SAMN17765873\n"
                + "SAMN17765874\n"
                + "SAMN17765875\n"
                + "SAMN17765876\n"
                + "SAMN17765877\n"
                + "SAMN17765879\n"
                + "SAMN17765880\n"
                + "SAMN17765878\n"
                + "SAMN17765881\n"
                + "SAMN17765882\n"
                + "SAMN17765883\n"
                + "SAMN17765884\n"
                + "SAMN17765885\n"
                + "SAMN17765887\n"
                + "SAMN17765888\n"
                + "SAMN17765889\n"
                + "SAMN17765890\n"
                + "SAMN17765886\n"
                + "SAMN17765891\n"
                + "SAMN17765892\n"
                + "SAMN17765894\n"
                + "SAMN17765899\n"
                + "SAMN17765893\n"
                + "SAMN17765895\n"
                + "SAMN17765896\n"
                + "SAMN17765897\n"
                + "SAMN17765898\n"
                + "SAMN17765900\n"
                + "SAMN28640157\n"
                + "SAMN28640158\n"
                + "SAMN28640159\n"
                + "SAMN28640161\n"
                + "SAMN28640162\n"
                + "SAMN28640164\n"
                + "SAMN28640160\n"
                + "SAMN28640163\n"
                + "SAMN28640165\n"
                + "SAMN28640166\n"
                + "SAMN29388701\n"
                + "SAMN29388704\n"
                + "SAMN29388705\n"
                + "SAMN29388707\n"
                + "SAMN29388789\n"
                + "SAMN29388790\n"
                + "SAMN29388791\n"
                + "SAMN29388792\n"
                + "SAMN29388795\n"
                + "SAMN29388796\n"
                + "SAMN29388797\n"
                + "SAMN29388798\n"
                + "SAMN29388799\n"
                + "SAMN29388768\n"
                + "SAMN29388769\n"
                + "SAMN29388770\n"
                + "SAMN29388757\n"
                + "SAMN29388758\n"
                + "SAMN29388759\n"
                + "SAMN29388760\n"
                + "SAMN29388761\n"
                + "SAMN29388762\n"
                + "SAMN29388763\n"
                + "SAMN29388764\n"
                + "SAMN29388765\n"
                + "SAMN29388766\n"
                + "SAMN29388690\n"
                + "SAMN29388691\n"
                + "SAMN29388692\n"
                + "SAMN29388693\n"
                + "SAMN29388694\n"
                + "SAMN29388695\n"
                + "SAMN29388696\n"
                + "SAMN29388697\n"
                + "SAMN29388698\n"
                + "SAMN29388699\n"
                + "SAMN29388709\n"
                + "SAMN29388710\n"
                + "SAMN29388711\n"
                + "SAMN29388712\n"
                + "SAMN29388713\n"
                + "SAMN29388714\n"
                + "SAMN29388715\n"
                + "SAMN29388801\n"
                + "SAMN29388803\n"
                + "SAMN29388804\n"
                + "SAMN29388805\n"
                + "SAMN29388806\n"
                + "SAMN29388807\n"
                + "SAMN29388808\n"
                + "SAMN29388756\n"
                + "SAMN29388771\n"
                + "SAMN29388778\n"
                + "SAMN29388830\n"
                + "SAMN29388782\n"
                + "SAMN29388783\n"
                + "SAMN29388784\n"
                + "SAMN29762657\n"
                + "SAMN29762658\n"
                + "SAMN29762659\n"
                + "SAMN29762660\n"
                + "SAMN29762661\n"
                + "SAMN29762662\n"
                + "SAMN29762663\n"
                + "SAMN29762664\n"
                + "SAMN29762665\n"
                + "SAMN29762666\n"
                + "SAMN29762667\n"
                + "SAMN29762668\n"
                + "SAMN29762669\n"
                + "SAMN29762670\n"
                + "SAMN29762671\n"
                + "SAMN29762672\n"
                + "SAMN29762673\n"
                + "SAMN29762674\n"
                + "SAMN29762675\n"
                + "SAMN29762676\n"
                + "SAMN29762647\n"
                + "SAMN29762648\n"
                + "SAMN29762649\n"
                + "SAMN29762650\n"
                + "SAMN29762651\n"
                + "SAMN29762652\n"
                + "SAMN29762653\n"
                + "SAMN29762654\n"
                + "SAMN29762655\n"
                + "SAMN29762656\n"
                + "SAMN29762687\n"
                + "SAMN29762688\n"
                + "SAMN29762689\n"
                + "SAMN29762690\n"
                + "SAMN29762691\n"
                + "SAMN29762692\n"
                + "SAMN29762693\n"
                + "SAMN29762694\n"
                + "SAMN29762695\n"
                + "SAMN29762696\n"
                + "SAMN29762697\n"
                + "SAMN29762698\n"
                + "SAMN29762699\n"
                + "SAMN29762700\n"
                + "SAMN29762701\n"
                + "SAMN29762702\n"
                + "SAMN29762703\n"
                + "SAMN29762704\n"
                + "SAMN29762705\n"
                + "SAMN29762706\n"
                + "SAMN29762681\n"
                + "SAMN29762682\n"
                + "SAMN29762683\n"
                + "SAMN29762684\n"
                + "SAMN29762685\n"
                + "SAMN29762686\n"
                + "SAMN29762678\n"
                + "SAMN29762677\n"
                + "SAMN29762679\n"
                + "SAMN29762680\n"
                + "SAMN31055682\n"
                + "SAMN31055693\n"
                + "SAMN31055696\n"
                + "SAMN31055697\n"
                + "SAMN31055741\n"
                + "SAMN31055742\n"
                + "SAMN31055691\n"
                + "SAMN31055692\n"
                + "SAMN31055695\n"
                + "SAMN31055738\n"
                + "SAMN31055739\n"
                + "SAMN31057811\n"
                + "SAMN31057983\n"
                + "SAMN31057984\n"
                + "SAMN31057994\n"
                + "SAMN31057996\n"
                + "SAMN31058001\n"
                + "SAMN31058002\n"
                + "SAMN31058009\n"
                + "SAMN31057989\n"
                + "SAMN31057993\n"
                + "SAMN31058010\n"
                + "SAMN31058011\n"
                + "SAMN31060414\n"
                + "SAMN31060415\n"
                + "SAMN31060416\n"
                + "SAMN31060417\n"
                + "SAMN31060419\n"
                + "SAMN31060428\n"
                + "SAMN31060429\n"
                + "SAMN31060430\n"
                + "SAMN31060431\n"
                + "SAMN31060418\n"
                + "SAMN31060433\n"
                + "SAMN31060436\n"
                + "SAMN31060437\n"
                + "SAMN31060438\n"
                + "SAMN31060439\n"
                + "SAMN31060441\n"
                + "SAMN31060432\n"
                + "SAMN31060434\n"
                + "SAMN31060435\n"
                + "SAMN31060440\n"
                + "SAMN31060444\n"
                + "SAMN31060445\n"
                + "SAMN31060446\n"
                + "SAMN31060447\n"
                + "SAMN31060448\n"
                + "SAMN31060455\n"
                + "SAMN31060456\n"
                + "SAMN31060663\n"
                + "SAMN31060664\n"
                + "SAMN31060442\n"
                + "SAMN31060443");

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
