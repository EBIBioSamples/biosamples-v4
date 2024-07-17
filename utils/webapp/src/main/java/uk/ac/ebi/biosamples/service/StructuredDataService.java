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
package uk.ac.ebi.biosamples.service;

import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.exceptions.GlobalExceptions;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.mongo.model.MongoStructuredData;
import uk.ac.ebi.biosamples.mongo.repository.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.repository.MongoStructuredDataRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoStructuredDataToStructuredDataConverter;
import uk.ac.ebi.biosamples.mongo.service.StructuredDataToMongoStructuredDataConverter;

@Service
public class StructuredDataService {
  private static final Logger log = LoggerFactory.getLogger(StructuredDataService.class);

  private final MongoSampleRepository mongoSampleRepository;
  private final MongoStructuredDataRepository mongoStructuredDataRepository;
  private final MongoStructuredDataToStructuredDataConverter
      mongoStructuredDataToStructuredDataConverter;
  private final StructuredDataToMongoStructuredDataConverter
      structuredDataToMongoStructuredDataConverter;
  private final MessagingService messagingService;

  public StructuredDataService(
      final MongoSampleRepository mongoSampleRepository,
      final MongoStructuredDataRepository mongoStructuredDataRepository,
      final MongoStructuredDataToStructuredDataConverter
          mongoStructuredDataToStructuredDataConverter,
      final StructuredDataToMongoStructuredDataConverter
          structuredDataToMongoStructuredDataConverter,
      final MessagingService messagingService) {
    this.mongoSampleRepository = mongoSampleRepository;
    this.mongoStructuredDataRepository = mongoStructuredDataRepository;
    this.mongoStructuredDataToStructuredDataConverter =
        mongoStructuredDataToStructuredDataConverter;
    this.structuredDataToMongoStructuredDataConverter =
        structuredDataToMongoStructuredDataConverter;
    this.messagingService = messagingService;
  }

  public Optional<StructuredData> getStructuredData(final String accession) {
    final Optional<MongoStructuredData> byId = mongoStructuredDataRepository.findById(accession);
    final MongoStructuredData mongoData = byId.orElse(null);

    if (mongoData == null) {
      return Optional.empty();
    }

    return Optional.of(mongoStructuredDataToStructuredDataConverter.convert(mongoData));
  }

  public StructuredData saveStructuredData(StructuredData structuredData) {
    validate(structuredData);
    Instant create = Instant.now();
    final Optional<MongoStructuredData> byId =
        mongoStructuredDataRepository.findById(structuredData.getAccession());
    final MongoStructuredData oldMongoData = byId.orElse(null);

    if (oldMongoData != null) {
      final StructuredData oldData =
          mongoStructuredDataToStructuredDataConverter.convert(oldMongoData);
      // we consider if domain and types are equal StructuredDataTable
      // holds the same data and changed equals method accordingly
      structuredData.getData().addAll(oldData.getData());
      create = oldData.getCreate();
    }

    structuredData =
        StructuredData.build(structuredData.getAccession(), create, structuredData.getData());
    MongoStructuredData mongoStructuredData =
        structuredDataToMongoStructuredDataConverter.convert(structuredData);
    mongoStructuredData = mongoStructuredDataRepository.save(mongoStructuredData);

    messagingService.fetchThenSendMessage(structuredData.getAccession());
    return mongoStructuredDataToStructuredDataConverter.convert(mongoStructuredData);
  }

  private void validate(final StructuredData structuredData) {
    if (structuredData.getAccession() == null
        || !isExistingAccession(structuredData.getAccession())) {
      log.info(
          "Structured data validation failed: Misisng accession {}", structuredData.getAccession());
      throw new GlobalExceptions.SampleValidationException(
          "Missing accession. Structured data should have an accession");
    }

    if (structuredData.getData() == null || structuredData.getData().isEmpty()) {
      log.info("Structured data validation failed: Misisng data {}", structuredData.getData());
      throw new GlobalExceptions.SampleValidationException(
          "Missing data. Empty data is not accepted");
    }

    structuredData
        .getData()
        .forEach(
            d -> {
              if (d.getType() == null || d.getType().isEmpty()) {
                log.info("Structured data validation failed: Misisng data type {}", d.getType());
                throw new GlobalExceptions.SampleValidationException(
                    "Empty structured data type. Type must be specified.");
              }
            });
  }

  private boolean isExistingAccession(final String accession) {
    return mongoSampleRepository.findById(accession).isPresent();
  }
}
