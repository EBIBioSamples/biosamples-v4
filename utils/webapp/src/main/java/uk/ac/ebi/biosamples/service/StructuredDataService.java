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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.ac.ebi.biosamples.exception.SampleNotFoundException;
import uk.ac.ebi.biosamples.exception.SampleValidationException;
import uk.ac.ebi.biosamples.model.Autocomplete;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.StaticViewWrapper;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;
import uk.ac.ebi.biosamples.mongo.model.MongoRelationship;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoStructuredData;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoStructuredDataRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoAccessionService;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.MongoStructuredDataToStructuredDataConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.SampleToMongoSampleStructuredDataCentricConverter;
import uk.ac.ebi.biosamples.mongo.service.StructuredDataToMongoStructuredDataConverter;
import uk.ac.ebi.biosamples.solr.service.SolrSampleService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class StructuredDataService {
  private static final Logger log = LoggerFactory.getLogger(StructuredDataService.class);

  private final SampleService sampleService;
  private final MongoStructuredDataRepository mongoStructuredDataRepository;
  private final MongoStructuredDataToStructuredDataConverter mongoStructuredDataToStructuredDataConverter;
  private final StructuredDataToMongoStructuredDataConverter structuredDataToMongoStructuredDataConverter;

  public StructuredDataService(SampleService sampleService,
                               MongoStructuredDataRepository mongoStructuredDataRepository,
                               MongoStructuredDataToStructuredDataConverter mongoStructuredDataToStructuredDataConverter,
                               StructuredDataToMongoStructuredDataConverter structuredDataToMongoStructuredDataConverter) {
    this.sampleService = sampleService;
    this.mongoStructuredDataRepository = mongoStructuredDataRepository;
    this.mongoStructuredDataToStructuredDataConverter = mongoStructuredDataToStructuredDataConverter;
    this.structuredDataToMongoStructuredDataConverter = structuredDataToMongoStructuredDataConverter;
  }

  public StructuredData getStructuredData(String accession) {
    MongoStructuredData mongoData = mongoStructuredDataRepository.findOne(accession);
    if (mongoData == null) {
      throw new SampleNotFoundException();
    }

    return mongoStructuredDataToStructuredDataConverter.convert(mongoData);
  }

  public StructuredData saveStructuredData(StructuredData structuredData) {
    validate(structuredData);
    Instant create = Instant.now();
    MongoStructuredData oldMongoData = mongoStructuredDataRepository.findOne(structuredData.getAccession());
    if (oldMongoData != null) {
      StructuredData oldData = mongoStructuredDataToStructuredDataConverter.convert(oldMongoData);
      // we consider if domain and types are equal StructuredDataTable
      // holds the same data and changed equals method accordingly
      structuredData.getData().addAll(oldData.getData());
      create = oldData.getCreate();
    }

    structuredData = StructuredData.build(structuredData.getAccession(), create, structuredData.getData());
    MongoStructuredData mongoStructuredData = structuredDataToMongoStructuredDataConverter.convert(structuredData);
    mongoStructuredData = mongoStructuredDataRepository.save(mongoStructuredData);

    return mongoStructuredDataToStructuredDataConverter.convert(mongoStructuredData);
  }

  private void validate(StructuredData structuredData) {
    if (structuredData.getAccession() == null || sampleService.isNotExistingAccession(structuredData.getAccession())) {
      log.info("Structured data validation failed: Misisng accession {}", structuredData.getAccession());
      throw new SampleValidationException("Missing accession. Structured data should have an accession");
    }

    if (structuredData.getData() == null || structuredData.getData().isEmpty()) {
      log.info("Structured data validation failed: Misisng data {}", structuredData.getData());
      throw new SampleValidationException("Missing data. Empty data is not accepted");
    }

    structuredData.getData().forEach(d -> {
      if (d.getType() == null || d.getType().isEmpty()) {
        log.info("Structured data validation failed: Misisng data type {}", d.getType());
        throw new SampleValidationException("Empty structured data type. Type must be specified.");
      }
    });
  }

}
