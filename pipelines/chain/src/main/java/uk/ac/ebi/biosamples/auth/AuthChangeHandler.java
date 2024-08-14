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
package uk.ac.ebi.biosamples.auth;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
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
  private static final String WEBIN_33870 = "Webin-33870";
  public static final String SELF_ATLANT_ECO = "self.AtlantECO";
  private final BioSamplesClient bioSamplesWebinClient;
  private final BioSamplesClient bioSamplesAapClient;
  private final MongoAuthChangeRepository mongoAuthChangeRepository;

  public AuthChangeHandler(
      @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesWebinClient,
      @Qualifier("AAPCLIENT") final BioSamplesClient bioSamplesAapClient,
      final MongoAuthChangeRepository mongoAuthChangeRepository) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.bioSamplesAapClient = bioSamplesAapClient;
    this.mongoAuthChangeRepository = mongoAuthChangeRepository;
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
      handleAuth(optionalSampleEntityModel);
    } else {
      log.info("Sample not found: " + accession);
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

    if (sampleDomain.equals(SELF_ATLANT_ECO)) {
      log.info(
          "Sample authority needs to change for: " + accession + " setting to: " + WEBIN_33870);

      final Sample updatedSample =
          Sample.Builder.fromSample(sample)
              .withWebinSubmissionAccountId(WEBIN_33870)
              .withNoDomain()
              .build();

      final EntityModel<Sample> sampleEntityModel =
          bioSamplesWebinClient.persistSampleResource(updatedSample);

      if (Objects.requireNonNull(sampleEntityModel.getContent())
          .getWebinSubmissionAccountId()
          .equals(WEBIN_33870)) {
        log.info("Sample " + accession + " updated");

        mongoAuthChangeRepository.save(
            new MongoAuthChangeRecord(accession, SELF_ATLANT_ECO, WEBIN_33870));
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

        processSample(sampleIdentifier, Collections.singletonList(""));
      }

    } catch (IOException e) {
      log.info("Failed to process CSV file " + e);
    }
  }
}
