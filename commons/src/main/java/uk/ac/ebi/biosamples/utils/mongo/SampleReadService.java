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
package uk.ac.ebi.biosamples.utils.mongo;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.StaticViewWrapper;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoStructuredData;
import uk.ac.ebi.biosamples.mongo.repo.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.repo.MongoStructuredDataRepository;
import uk.ac.ebi.biosamples.mongo.service.MongoInverseRelationshipService;
import uk.ac.ebi.biosamples.mongo.service.MongoSampleToSampleConverter;
import uk.ac.ebi.biosamples.mongo.service.MongoStructuredDataToStructuredDataConverter;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;

/**
 * Service layer business logic for centralising repository access and conversions between different
 * controller. Use this instead of linking to repositories directly.
 *
 * @author faulcon
 */
@Service
public class SampleReadService {
  private static Logger LOGGER = LoggerFactory.getLogger(SampleReadService.class);
  private final MongoSampleRepository mongoSampleRepository;

  // TODO use a ConversionService to manage all these
  private final MongoSampleToSampleConverter mongoSampleToSampleConverter;

  private final CurationReadService curationReadService;
  private final MongoInverseRelationshipService mongoInverseRelationshipService;
  private final MongoStructuredDataRepository mongoStructuredDataRepository;
  private final MongoStructuredDataToStructuredDataConverter
      mongoStructuredDataToStructuredDataConverter;

  private final ExecutorService executorService;

  public SampleReadService(
      MongoSampleRepository mongoSampleRepository,
      MongoSampleToSampleConverter mongoSampleToSampleConverter,
      CurationReadService curationReadService,
      MongoInverseRelationshipService mongoInverseRelationshipService,
      MongoStructuredDataRepository mongoStructuredDataRepository,
      MongoStructuredDataToStructuredDataConverter mongoStructuredDataToStructuredDataConverter,
      BioSamplesProperties bioSamplesProperties) {
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
  public Optional<Sample> fetch(String accession, Optional<List<String>> curationDomains)
      throws IllegalArgumentException {
    // return the sample from the repository
    long startTime, endTime;

    startTime = System.nanoTime();

    final Optional<MongoSample> byId = mongoSampleRepository.findById(accession);
    MongoSample mongoSample = byId.orElse(null);

    if (mongoSample == null) {
      LOGGER.warn(String.format("failed to retrieve sample with accession %s", accession));
      return Optional.empty();
    }
    endTime = System.nanoTime();
    LOGGER.trace(
        "Got mongo original " + accession + " in " + ((endTime - startTime) / 1000000) + "ms");

    // add on inverse relationships
    startTime = System.nanoTime();
    mongoSample = mongoInverseRelationshipService.addInverseRelationships(mongoSample);
    endTime = System.nanoTime();
    LOGGER.trace(
        "Got inverse relationships "
            + accession
            + " in "
            + ((endTime - startTime) / 1000000)
            + "ms");

    // convert it into the format to return
    Sample sample = mongoSampleToSampleConverter.apply(mongoSample);

    // add curation from a set of users
    startTime = System.nanoTime();
    sample = curationReadService.applyAllCurationToSample(sample, curationDomains);
    endTime = System.nanoTime();
    LOGGER.trace(
        "Applied curation to " + accession + " in " + ((endTime - startTime) / 1000000) + "ms");

    return Optional.of(sample);
  }

  public Optional<Sample> fetch(
      String accession,
      Optional<List<String>> curationDomains,
      StaticViewWrapper.StaticView staticViews) {

    Sample sample;
    MongoSample mongoSample =
        mongoSampleRepository.findSampleFromCollection(accession, staticViews);

    if (mongoSample == null) {
      LOGGER.warn("failed to retrieve sample with accession {}", accession);
      sample = null;
    } else if (staticViews.equals(StaticViewWrapper.StaticView.SAMPLES_DYNAMIC)) {
      mongoSample = mongoInverseRelationshipService.addInverseRelationships(mongoSample);
      sample = mongoSampleToSampleConverter.apply(mongoSample);
      sample = curationReadService.applyAllCurationToSample(sample, curationDomains);
    } else {
      sample = mongoSampleToSampleConverter.apply(mongoSample);
    }

    // todo add structured data
    final Optional<MongoStructuredData> byId = mongoStructuredDataRepository.findById(accession);
    MongoStructuredData mongoStructuredData = byId.orElse(null);

    if (mongoStructuredData != null) {
      StructuredData structuredData =
          mongoStructuredDataToStructuredDataConverter.convert(mongoStructuredData);
      assert sample != null;
      sample =
          Sample.Builder.fromSample(sample).withStructuredData(structuredData.getData()).build();
    }

    return sample == null ? Optional.empty() : Optional.of(sample);
  }

  public Future<Optional<Sample>> fetchAsync(
      String accession, Optional<List<String>> curationDomains) {
    return executorService.submit(new FetchCallable(accession, this, curationDomains));
  }

  public Future<Optional<Sample>> fetchAsync(
      String accession,
      Optional<List<String>> curationDomains,
      StaticViewWrapper.StaticView staticViews) {
    return executorService.submit(() -> fetch(accession, curationDomains, staticViews));
  }

  private static class FetchCallable implements Callable<Optional<Sample>> {
    private final SampleReadService sampleReadService;
    private final String accession;
    private final Optional<List<String>> curationDomains;

    public FetchCallable(
        String accession,
        SampleReadService sampleReadService,
        Optional<List<String>> curationDomains) {
      this.accession = accession;
      this.sampleReadService = sampleReadService;
      this.curationDomains = curationDomains;
    }

    @Override
    public Optional<Sample> call() {
      Optional<Sample> opt = sampleReadService.fetch(accession, curationDomains);
      if (!opt.isPresent()) {
        LOGGER.warn(String.format("failed to retrieve sample with accession %s", accession));
      }
      return opt;
    }
  }
}
