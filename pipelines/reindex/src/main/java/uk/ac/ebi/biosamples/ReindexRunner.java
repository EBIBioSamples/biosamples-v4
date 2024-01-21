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
package uk.ac.ebi.biosamples;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import org.apache.commons.lang.IncompleteArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.filter.DateRangeFilter;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.mongo.model.MongoAccessionMapping;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.mongo.repo.MongoAccessionMappingRepository;
import uk.ac.ebi.biosamples.mongo.service.SampleReadService;
import uk.ac.ebi.biosamples.utils.BioSamplesConstants;
import uk.ac.ebi.biosamples.utils.PipelineUtils;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

/**
 * This runner will get a list of accessions from mongo directly, query the API to get the latest
 * information, and then send that information to Rabbit for the Solr Agent to reindex it into Solr.
 *
 * <p>Mongo is queried instead of the API because the API is driven by Solr, and if Solr is
 * incorrect (which it will be because why else would you run this) then it won't get the right
 * information from the API.
 *
 * @author faulcon
 */
@Component
public class ReindexRunner implements ApplicationRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);
  private final AmqpTemplate amqpTemplate;
  private final SampleReadService sampleReadService;
  private final MongoOperations mongoOperations;
  private final MongoAccessionMappingRepository mongoAccessionMappingRepository;

  @Autowired
  public ReindexRunner(
      final AmqpTemplate amqpTemplate,
      final SampleReadService sampleReadService,
      final MongoOperations mongoOperations,
      final MongoAccessionMappingRepository mongoAccessionMappingRepository) {
    this.amqpTemplate = amqpTemplate;
    this.sampleReadService = sampleReadService;
    this.mongoOperations = mongoOperations;
    this.mongoAccessionMappingRepository = mongoAccessionMappingRepository;
  }

  @Override
  public void run(final ApplicationArguments args) throws Exception {
    final Collection<Filter> filters = PipelineUtils.getDateFilters(args, "update");
    final Map<String, Future<Void>> futures = new HashMap<>();

    ExecutorService executor = null;
    try {
      executor = Executors.newFixedThreadPool(128);

      final Query query = new Query();
      try {
        final DateRangeFilter filter =
            (DateRangeFilter)
                filters.stream()
                    .findFirst()
                    .orElseThrow(() -> new IncompleteArgumentException("Filters not found"));
        final DateRangeFilter.DateRange dateRange =
            filter
                .getContent()
                .orElseThrow(() -> new IncompleteArgumentException("Filters not found"));
        query.addCriteria(
            Criteria.where("update").gte(dateRange.getFrom()).lt(dateRange.getUntil()));
        LOGGER.info(
            "Found date filters. Starting reindex from "
                + dateRange.getFrom()
                + " to "
                + dateRange.getUntil());
      } catch (final IncompleteArgumentException e) {
        LOGGER.warn("Date filters are not present. Starting reindex from the beginning of time.");
      }

      try (final CloseableIterator<MongoSample> it =
          mongoOperations.stream(query, MongoSample.class)) {
        while (it.hasNext()) {
          final MongoSample mongoSample = it.next();
          final String accession = mongoSample.getAccession();

          LOGGER.info("Handling sample " + accession);

          final List<Attribute> sraAccessions =
              mongoSample.getAttributes().stream()
                  .filter(
                      attribute -> attribute.getType().equals(BioSamplesConstants.SRA_ACCESSION))
                  .collect(Collectors.toList());

          futures.put(
              accession,
              executor.submit(
                  new AccessionMappingAndIndexingCallable(
                      accession,
                      sraAccessions,
                      mongoAccessionMappingRepository,
                      sampleReadService,
                      amqpTemplate)));

          ThreadUtils.checkFutures(futures, 1000);
        }
      }

      ThreadUtils.checkFutures(futures, 0);
    } finally {
      executor.shutdown();
      executor.awaitTermination(24, TimeUnit.HOURS);
    }
  }

  private static class AccessionMappingAndIndexingCallable implements Callable<Void> {
    private static final List<Sample> related = new ArrayList<>();
    private final String accession;
    private final List<Attribute> sraAccessions;
    private final MongoAccessionMappingRepository mongoAccessionMappingRepository;
    private final SampleReadService sampleReadService;
    private final AmqpTemplate amqpTemplate;

    public AccessionMappingAndIndexingCallable(
        final String accession,
        final List<Attribute> sraAccessions,
        final MongoAccessionMappingRepository mongoAccessionMappingRepository,
        final SampleReadService sampleReadService,
        final AmqpTemplate amqpTemplate) {
      this.accession = accession;
      this.sraAccessions = sraAccessions;
      this.mongoAccessionMappingRepository = mongoAccessionMappingRepository;
      this.sampleReadService = sampleReadService;
      this.amqpTemplate = amqpTemplate;
    }

    @Override
    public Void call() {
      // additional feature to map SRA and SAME accessions while reindexing BioSamples database
      mapSRAAndSAMEAccession();

      if (!fetchSampleAndSendMessage(false)) {
        fetchSampleAndSendMessage(true);
      }

      return null;
    }

    private void mapSRAAndSAMEAccession() {
      if (sraAccessions.isEmpty()) {
        LOGGER.info("SRA accession doesn't exist for sample " + accession);
        return;
      }

      LOGGER.info("SRA accession exists for sample " + accession);

      if (sraAccessions.size() == 1) {
        mongoAccessionMappingRepository.save(
            new MongoAccessionMapping(sraAccessions.get(0).getValue(), accession));
      } else {
        Optional<Attribute> validSraAccessionAttribute =
            sraAccessions.stream()
                .filter(
                    sraAccessionAttribute -> isValidAccessionType(sraAccessionAttribute.getType()))
                .findFirst();

        validSraAccessionAttribute.ifPresent(
            attribute ->
                mongoAccessionMappingRepository.save(
                    new MongoAccessionMapping(attribute.getValue(), accession)));
      }
    }

    private boolean isValidAccessionType(String attributeType) {
      return attributeType != null
          && (attributeType.startsWith("ERS")
              || attributeType.startsWith("SRS")
              || attributeType.startsWith("DRS"));
    }

    private boolean fetchSampleAndSendMessage(final boolean isRetry) {
      if (isRetry) {
        try {
          TimeUnit.SECONDS.sleep(1);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      final Optional<Sample> sampleOptional = sampleReadService.fetch(accession, Optional.empty());

      if (sampleOptional.isPresent()) {
        try {
          amqpTemplate.convertAndSend(
              Messaging.REINDEXING_EXCHANGE,
              Messaging.REINDEXING_QUEUE,
              MessageContent.build(sampleOptional.get(), null, related, false));

          return true;
        } catch (final Exception e) {
          LOGGER.error(
              String.format(
                  "Failed to convert sample to message and send to queue for %s", accession),
              e);
        }
      } else {
        final String errorMessage =
            isRetry
                ? String.format("Failed to fetch sample after retrying for %s", accession)
                : String.format("Failed to fetch sample for %s", accession);

        LOGGER.warn(errorMessage);
      }

      return false;
    }
  }
}
