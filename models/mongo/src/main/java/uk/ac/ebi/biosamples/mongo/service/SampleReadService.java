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
package uk.ac.ebi.biosamples.mongo.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoStructuredData;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoStructuredDataRepository;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;

/**
 * Service layer business logic for centralising repository access and conversions between different
 * controller. Use this instead of linking to repositories directly.
 *
 * @author faulcon
 */
@Service
public class SampleReadService {
  private static final Logger LOGGER = LoggerFactory.getLogger(SampleReadService.class);
  private final MongoSampleRepository mongoSampleRepository;
  private final MongoSampleToSampleConverter mongoSampleToSampleConverter;
  private final CurationReadService curationReadService;
  private final MongoInverseRelationshipService mongoInverseRelationshipService;
  private final MongoStructuredDataRepository mongoStructuredDataRepository;
  private final MongoStructuredDataToStructuredDataConverter
      mongoStructuredDataToStructuredDataConverter;
  private final ExecutorService executorService;

  public SampleReadService(
      final MongoSampleRepository mongoSampleRepository,
      final MongoSampleToSampleConverter mongoSampleToSampleConverter,
      final CurationReadService curationReadService,
      final MongoInverseRelationshipService mongoInverseRelationshipService,
      final MongoStructuredDataRepository mongoStructuredDataRepository,
      final MongoStructuredDataToStructuredDataConverter
          mongoStructuredDataToStructuredDataConverter,
      final BioSamplesProperties bioSamplesProperties) {
    this.mongoSampleRepository = mongoSampleRepository;
    this.mongoSampleToSampleConverter = mongoSampleToSampleConverter;
    this.curationReadService = curationReadService;
    this.mongoInverseRelationshipService = mongoInverseRelationshipService;
    this.mongoStructuredDataRepository = mongoStructuredDataRepository;
    this.mongoStructuredDataToStructuredDataConverter =
        mongoStructuredDataToStructuredDataConverter;
    executorService =
        AdaptiveThreadPoolExecutor.create(
            10000,
            1000,
            false,
            bioSamplesProperties.getBiosamplesCorePageThreadCount(),
            bioSamplesProperties.getBiosamplesCorePageThreadCountMax());
  }

  /** Throws an IllegalArgumentException of no sample with that accession exists */
  // can't use a sync cache because we need to use CacheEvict
  // @Cacheable(cacheNames=WebappProperties.fetchUsing, key="#root.args[0]")
  public Optional<Sample> fetch(
      final String accession, final Optional<List<String>> curationDomains)
      throws IllegalArgumentException {
    MongoSample mongoSample = mongoSampleRepository.findById(accession).orElse(null);

    if (mongoSample == null) {
      LOGGER.warn(String.format("failed to retrieve sample with accession %s", accession));
      return Optional.empty();
    }

    // add on inverse relationships
    mongoSample = mongoInverseRelationshipService.addInverseRelationships(mongoSample);

    // convert it into the format to return
    Sample sample = mongoSampleToSampleConverter.apply(mongoSample);

    // add curation from a set of users
    sample = curationReadService.applyAllCurationToSample(sample, curationDomains);

    // add structured data
    final Optional<MongoStructuredData> mongoStructuredData =
        mongoStructuredDataRepository.findById(accession);

    if (mongoStructuredData.isPresent()) {
      final StructuredData structuredData =
          mongoStructuredDataToStructuredDataConverter.convert(mongoStructuredData.get());
      sample =
          Sample.Builder.fromSample(sample).withStructuredData(structuredData.getData()).build();
    }

    return Optional.of(sample);
  }

  /** Throws an IllegalArgumentException of no sample with that accession exists */
  // can't use a sync cache because we need to use CacheEvict
  // @Cacheable(cacheNames=WebappProperties.fetchUsing, key="#root.args[0]")
  public Optional<Sample> fetchWithMissingSraAccessionsAdded(
      final String accession, final Optional<List<String>> curationDomains)
      throws IllegalArgumentException {
    MongoSample mongoSample = mongoSampleRepository.findById(accession).orElse(null);

    if (mongoSample == null) {
      LOGGER.warn(String.format("failed to retrieve sample with accession %s", accession));
      return Optional.empty();
    }

    final Optional<Attribute> sraAccessionOptional =
        mongoSample.getAttributes().stream()
            .filter(attribute -> attribute.getType().equals("SRA accession"))
            .findAny();

    if (sraAccessionOptional.isEmpty()) {}

    // add on inverse relationships
    mongoSample = mongoInverseRelationshipService.addInverseRelationships(mongoSample);

    // convert it into the format to return
    Sample sample = mongoSampleToSampleConverter.apply(mongoSample);

    // add curation from a set of users
    sample = curationReadService.applyAllCurationToSample(sample, curationDomains);

    // add structured data
    final Optional<MongoStructuredData> mongoStructuredData =
        mongoStructuredDataRepository.findById(accession);

    if (mongoStructuredData.isPresent()) {
      final StructuredData structuredData =
          mongoStructuredDataToStructuredDataConverter.convert(mongoStructuredData.get());
      sample =
          Sample.Builder.fromSample(sample).withStructuredData(structuredData.getData()).build();
    }

    return Optional.of(sample);
  }

  public Future<Optional<Sample>> fetchAsync(
      final String accession, final Optional<List<String>> curationDomains) {
    return executorService.submit(new FetchCallable(accession, this, curationDomains));
  }

  private static class FetchCallable implements Callable<Optional<Sample>> {
    private final SampleReadService sampleReadService;
    private final String accession;
    private final Optional<List<String>> curationDomains;

    FetchCallable(
        final String accession,
        final SampleReadService sampleReadService,
        final Optional<List<String>> curationDomains) {
      this.accession = accession;
      this.sampleReadService = sampleReadService;
      this.curationDomains = curationDomains;
    }

    @Override
    public Optional<Sample> call() {
      final Optional<Sample> opt = sampleReadService.fetch(accession, curationDomains);
      if (!opt.isPresent()) {
        LOGGER.warn(String.format("failed to retrieve sample with accession %s", accession));
      }
      return opt;
    }
  }
}