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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesConstants;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.model.MongoStructuredData;
import uk.ac.ebi.biosamples.mongo.repository.MongoSampleRepository;
import uk.ac.ebi.biosamples.mongo.repository.MongoStructuredDataRepository;
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

  public Optional<Sample> fetch(final String accession, final boolean applyCurations) {
    final MongoSample mongoSample = getMongoSample(accession);

    if (mongoSample == null) {
      LOGGER.warn(String.format("1 - Failed to retrieve sample with accession %s", accession));

      return Optional.empty();
    }

    final MongoSample updatedMongoSample =
        mongoInverseRelationshipService.addInverseRelationships(mongoSample);
    final AtomicReference<Sample> sample =
        new AtomicReference<>(mongoSampleToSampleConverter.apply(updatedMongoSample));

    if (applyCurations) {
      sample.set(curationReadService.applyAllCurationToSample(sample.get()));
    }

    final Optional<MongoStructuredData> mongoStructuredData =
        mongoStructuredDataRepository.findById(accession);

    mongoStructuredData.ifPresent(
        data -> {
          final StructuredData structuredData =
              mongoStructuredDataToStructuredDataConverter.convert(data);
          sample.set(
              Sample.Builder.fromSample(sample.get())
                  .withStructuredData(structuredData.getData())
                  .build());
        });

    return Optional.of(sample.get());
  }

  private MongoSample getMongoSample(final String accession) {
    if (startsWithSraPrefix(accession)) {
      final List<MongoSample> samples = /*mongoSampleRepository.findBySraAccession(accession);*/
          mongoSampleRepository.findUsingSraAccessionIndex(accession);

      /*if (samples == null || samples.isEmpty()) {
        samples = mongoSampleRepository.findUsingSraAccessionIndex(accession);
      }*/

      return samples.isEmpty() ? null : samples.get(0);
    } else {
      return mongoSampleRepository.findById(accession).orElse(null);
    }
  }

  private boolean startsWithSraPrefix(final String accession) {
    return Arrays.stream(BioSamplesConstants.sraSampleAccessionPrefixesString)
        .anyMatch(accession::startsWith);
  }

  public Future<Optional<Sample>> fetchAsync(final String accession, final boolean applyCurations) {
    return executorService.submit(new FetchCallable(accession, this, applyCurations));
  }

  private static class FetchCallable implements Callable<Optional<Sample>> {
    private final SampleReadService sampleReadService;
    private final String accession;
    private final boolean applyCurations;

    FetchCallable(
        final String accession,
        final SampleReadService sampleReadService,
        final boolean applyCurations) {
      this.accession = accession;
      this.sampleReadService = sampleReadService;
      this.applyCurations = applyCurations;
    }

    @Override
    public Optional<Sample> call() {
      final Optional<Sample> opt = sampleReadService.fetch(accession, applyCurations);

      if (!opt.isPresent()) {
        LOGGER.warn(String.format("failed to retrieve sample with accession %s", accession));
      }

      return opt;
    }
  }
}
