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
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoAuthChangeRecord;
import uk.ac.ebi.biosamples.mongo.repository.MongoAuthChangeRepository;

@Component
public class StaticAuthChangeHandler {
  private static final Logger log = LoggerFactory.getLogger(StaticAuthChangeHandler.class);
  private final BioSamplesClient bioSamplesWebinClient;
  private final MongoAuthChangeRepository mongoAuthChangeRepository;

  public StaticAuthChangeHandler(
      @Qualifier("WEBINCLIENT") final BioSamplesClient bioSamplesWebinClient,
      final MongoAuthChangeRepository mongoAuthChangeRepository) {
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.mongoAuthChangeRepository = mongoAuthChangeRepository;
  }

  private void processSample(
      String accession,
      final String aapDomainToChange,
      final String webinIdCurrent,
      final String webinIdToChange) {
    if (!accession.startsWith("SAMEA")) {
      accession = "SAMEA" + accession;
    }

    log.info("Processing Sample: " + accession);

    final Optional<EntityModel<Sample>> optionalSampleEntityModel =
        bioSamplesWebinClient.fetchSampleResource(accession, false);

    if (optionalSampleEntityModel.isPresent()) {
      handleAuth(optionalSampleEntityModel, aapDomainToChange, webinIdCurrent, webinIdToChange);
    } else {
      log.info("Sample not found: " + accession);
    }
  }

  private void handleAuth(
      final Optional<EntityModel<Sample>> optionalSampleEntityModel,
      final String aapDomainToChange,
      final String webinIdCurrent,
      final String webinIdToChange) {
    final Sample sample = optionalSampleEntityModel.get().getContent();

    if (sample == null) {
      return;
    }

    final String accession = sample.getAccession();
    final String sampleDomain = sample.getDomain();
    final String webinId = sample.getWebinSubmissionAccountId();

    /*if (sample.getSubmittedVia() == SubmittedViaType.PIPELINE_IMPORT
        || sample.getSubmittedVia() == SubmittedViaType.WEBIN_SERVICES) {
      log.info("Sample is an imported sample: " + accession + " update at source");

      return;
    }*/

    boolean webinIdChange = webinId != null;

    if ((sampleDomain != null && sampleDomain.equals(aapDomainToChange))
        || (webinIdChange && sample.getWebinSubmissionAccountId().equals(webinIdCurrent))) {
      log.info(
          "Sample authority needs to change for: " + accession + " setting to: " + webinIdToChange);

      final Sample updatedSample =
          Sample.Builder.fromSample(sample)
              .withWebinSubmissionAccountId(webinIdToChange)
              .withNoDomain()
              .build();

      final EntityModel<Sample> sampleEntityModel =
          bioSamplesWebinClient.persistSampleResource(updatedSample);

      if (Objects.requireNonNull(sampleEntityModel.getContent())
          .getWebinSubmissionAccountId()
          .equals(webinIdToChange)) {
        log.info("Sample " + accession + " updated");

        if (!webinIdChange) {
          mongoAuthChangeRepository.save(
              new MongoAuthChangeRecord(accession, aapDomainToChange, webinIdToChange));
        }
      } else {
        log.info("Sample " + accession + " failed to be updated");
      }
    } else {
      log.info("Sample from some other domain, no change required");
    }
  }

  public void parseCsvAndProcessSampleAuthentication(
      final String aapDomainToChange, final String webinIdCurrent, final String webinIdToChange) {
    final String csvFile = "/mnt/data/biosamples/sw/www/samples.csv";

    try (final BufferedReader reader = new BufferedReader(new FileReader(csvFile));
        final CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader())) {

      for (final CSVRecord csvRecord : csvParser) {
        final String sampleIdentifier = csvRecord.get("Sample Identifier");

        processSample(sampleIdentifier, aapDomainToChange, webinIdCurrent, webinIdToChange);
      }

    } catch (IOException e) {
      log.info("Failed to process CSV file " + e);
    }
  }

  public void parseListOfSamplesAndProcessSampleAuthentication(
      final List<String> samples,
      final String aapDomainToChange,
      final String webinIdCurrent,
      final String webinIdToChange) {
    try {
      samples.forEach(
          sample -> processSample(sample, aapDomainToChange, webinIdCurrent, webinIdToChange));
    } catch (Exception e) {
      log.info("Failed to process list of samples " + e);
    }
  }

  public void parseFileAndProcessSampleAuthentication(
      final String filePath,
      final String aapDomainToChange,
      final String webinIdCurrent,
      final String webinIdToChange) {
    try (final BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String identifier;

      while ((identifier = br.readLine()) != null) {
        processSample(identifier.trim(), aapDomainToChange, webinIdCurrent, webinIdToChange);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to process file: " + filePath, e);
    }
  }
}
