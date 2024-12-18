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
package uk.ac.ebi.biosamples.auth.services;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.mongo.model.MongoAuthChangeRecord;
import uk.ac.ebi.biosamples.mongo.repository.MongoAuthChangeRepository;

@Component
public class AuthChangeHandler {
  private static final Logger log = LoggerFactory.getLogger(AuthChangeHandler.class);
  private static final String WEBIN_ID_TO_CHANGE_TO = "Webin-64171";
  public static final String AAP_DOMAIN_TO_CHANGE = "self.AtlantECO";
  private final BioSamplesClient bioSamplesWebinClient;
  private final MongoAuthChangeRepository mongoAuthChangeRepository;

  public AuthChangeHandler(
      @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesWebinClient,
      final MongoAuthChangeRepository mongoAuthChangeRepository) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.mongoAuthChangeRepository = mongoAuthChangeRepository;
  }

  private void processSample(final String accession) {
    if (!accession.startsWith("SAMEA")) {
      final String sameaAccession = "SAMEA" + accession;

      log.info("Processing Sample: " + sameaAccession);

      final Optional<EntityModel<Sample>> optionalSampleEntityModel =
          bioSamplesWebinClient.fetchSampleResource(sameaAccession, false);

      if (optionalSampleEntityModel.isPresent()) {
        handleAuth(optionalSampleEntityModel);
      } else {
        log.info("Sample not found: " + accession);
      }
    }
  }

  private void handleAuth(final Optional<EntityModel<Sample>> optionalSampleEntityModel) {
    final Sample sample = optionalSampleEntityModel.get().getContent();

    if (sample == null) {
      return;
    }

    final String accession = sample.getAccession();
    final String sampleDomain = sample.getDomain();

    if (sample.getSubmittedVia() == SubmittedViaType.PIPELINE_IMPORT
        || sample.getSubmittedVia() == SubmittedViaType.WEBIN_SERVICES) {
      log.info("Sample is an imported sample: " + accession + " update at source");

      return;
    }

    if (sampleDomain.equals(AAP_DOMAIN_TO_CHANGE)) {
      log.info(
          "Sample authority needs to change for: "
              + accession
              + " setting to: "
              + WEBIN_ID_TO_CHANGE_TO);

      final Sample updatedSample =
          Sample.Builder.fromSample(sample)
              .withWebinSubmissionAccountId(WEBIN_ID_TO_CHANGE_TO)
              .withNoDomain()
              .build();

      final EntityModel<Sample> sampleEntityModel =
          bioSamplesWebinClient.persistSampleResource(updatedSample);

      if (Objects.requireNonNull(sampleEntityModel.getContent())
          .getWebinSubmissionAccountId()
          .equals(WEBIN_ID_TO_CHANGE_TO)) {
        log.info("Sample " + accession + " updated");

        mongoAuthChangeRepository.save(
            new MongoAuthChangeRecord(accession, AAP_DOMAIN_TO_CHANGE, WEBIN_ID_TO_CHANGE_TO));
      } else {
        log.info("Sample " + accession + " failed to be updated");
      }
    } else {
      log.info("Sample from some other domain, no change required");
    }
  }

  public void parseCsvAndProcessSampleAuthentication() {
    final String csvFile = "/mnt/data/biosamples/sw/www/samples.csv";

    try (final BufferedReader reader = new BufferedReader(new FileReader(csvFile));
        final CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader())) {

      for (final CSVRecord csvRecord : csvParser) {
        final String sampleIdentifier = csvRecord.get("Sample Identifier");

        processSample(sampleIdentifier);
      }

    } catch (IOException e) {
      log.info("Failed to process CSV file " + e);
    }
  }

  public void parseFileAndProcessSampleAuthentication() {
    final String file = "C:\\Users\\dgupta\\BioSamples_ownership-change-to-Webin-64171.txt";

    try (final BufferedReader br = new BufferedReader(new FileReader(file))) {
      String identifier;

      while ((identifier = br.readLine()) != null) {
        processSample(identifier);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void parseListOfSamplesAndProcessSampleAuthentication() {
    final List<String> samples =
        List.of(
            "SAMEA8231217",
            "SAMEA8231218",
            "SAMEA8231219",
            "SAMEA8231220",
            "SAMEA8231221",
            "SAMEA8231222",
            "SAMEA8231223",
            "SAMEA8231224",
            "SAMEA8231225",
            "SAMEA8231226");

    try {
      samples.forEach(sample -> processSample(sample));
    } catch (Exception e) {
      log.info("Failed to process list of samples " + e);
    }
  }
}
