/*
* Copyright 2019 EMBL - European Bioinformatics Institute
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoSample;
import uk.ac.ebi.biosamples.service.SampleReadService;
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

  @Autowired
  public ReindexRunner(
      AmqpTemplate amqpTemplate,
      SampleReadService sampleReadService,
      MongoOperations mongoOperations) {
    this.amqpTemplate = amqpTemplate;
    this.sampleReadService = sampleReadService;
    this.mongoOperations = mongoOperations;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    Map<String, Future<Void>> futures = new HashMap<>();

    ExecutorService executor = null;
    try {
      executor = Executors.newFixedThreadPool(128);

      List<MongoSample> mongoSamples = mongoOperations.find(new Query(), MongoSample.class);
      ExecutorService finalExecutor = executor;

      mongoSamples.forEach(
          mongoSample -> {
            String accession = mongoSample.getAccession();
            LOGGER.info("handling sample " + accession);
            futures.put(
                accession,
                finalExecutor.submit(
                    new AccessionCallable(accession, sampleReadService, amqpTemplate)));
          });

      ThreadUtils.checkFutures(futures, 0);
    } finally {
      executor.shutdown();
      executor.awaitTermination(24, TimeUnit.HOURS);
    }
  }

  private static class AccessionCallable implements Callable<Void> {

    private final String accession;
    private final SampleReadService sampleReadService;
    private final AmqpTemplate amqpTemplate;
    private static final List<Sample> related = new ArrayList<>();

    public AccessionCallable(
        String accession, SampleReadService sampleReadService, AmqpTemplate amqpTemplate) {
      this.accession = accession;
      this.sampleReadService = sampleReadService;
      this.amqpTemplate = amqpTemplate;
    }

    @Override
    public Void call() throws Exception {
      if (!fetchSampleAndSendMessage(false)) {
        fetchSampleAndSendMessage(true);
      }
      return null;
    }

    private boolean fetchSampleAndSendMessage(boolean isRetry) {
      if (isRetry) {
        try {
          TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      Optional<Sample> opt = sampleReadService.fetch(accession, Optional.empty());
      if (opt.isPresent()) {
        try {
          Sample sample = opt.get();
          MessageContent messageContent = MessageContent.build(sample, null, related, false);
          amqpTemplate.convertAndSend(Messaging.exchangeForIndexingSolr, "", messageContent);
          return true;
        } catch (Exception e) {
          LOGGER.error(
              String.format(
                  "failed to convert sample to message and send to queue for %s", accession));
          return false;
        }
      } else {
        if (isRetry) {
          LOGGER.error(String.format("failed to fetch sample after retrying for %s", accession));
        } else {
          LOGGER.warn(String.format("failed to fetch sample for %s", accession));
        }
        return false;
      }
    }
  }
}
