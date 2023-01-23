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

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

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
import uk.ac.ebi.biosamples.client.service.StructuredDataSubmissionService;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;
import uk.ac.ebi.biosamples.mongo.model.MongoStructuredData;
import uk.ac.ebi.biosamples.mongo.repo.MongoStructuredDataRepository;
import uk.ac.ebi.biosamples.utils.ThreadUtils;
import uk.ac.ebi.biosamples.utils.mongo.SampleReadService;

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
public class AmrCleanRunner implements ApplicationRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);
  private final MongoStructuredDataRepository mongoStructuredDataRepository;
  private final MongoOperations mongoOperations;

  @Autowired
  public AmrCleanRunner(
      MongoStructuredDataRepository mongoStructuredDataRepository, MongoOperations mongoOperations) {
    this.mongoStructuredDataRepository = mongoStructuredDataRepository;
    this.mongoOperations = mongoOperations;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    Map<String, Future<Void>> futures = new HashMap<>();
    ExecutorService executor = null;

    try {
      executor = Executors.newFixedThreadPool(128);

      final Query query = new Query();
      try (CloseableIterator<MongoStructuredData> it = mongoOperations.stream(query, MongoStructuredData.class)) {
        while (it.hasNext()) {
          MongoStructuredData structuredData = it.next();
          String accession = structuredData.getAccession();
          LOGGER.info("Handling structured data " + accession);
          futures.put(
              accession,
              executor.submit(new SdCleanCallable(structuredData, mongoStructuredDataRepository)));
          ThreadUtils.checkFutures(futures, 1000);
        }
      }
      ThreadUtils.checkFutures(futures, 0);
      Thread.sleep(1000);
    } finally {
      executor.shutdown();
      executor.awaitTermination(24, TimeUnit.HOURS);
    }
  }

  private static class SdCleanCallable implements Callable<Void> {
    private final MongoStructuredData structuredData;
    private final MongoStructuredDataRepository mongoStructuredDataRepository;

    public SdCleanCallable(MongoStructuredData structuredData,
                           MongoStructuredDataRepository mongoStructuredDataRepository) {
      this.structuredData = structuredData;
      this.mongoStructuredDataRepository = mongoStructuredDataRepository;
    }

    @Override
    public Void call() {
      Set<StructuredDataTable> dataSet = structuredData.getData();
      Set<StructuredDataTable> enaImportedData = new HashSet<>();
      Set<StructuredDataTable> emptyDomainAndWebinData = new HashSet<>();
      int originalSize = dataSet.size();

      for (StructuredDataTable data : dataSet) {
        if ("Webin-40894".equals(data.getWebinSubmissionAccountId())) {
          LOGGER.info("Found ENA imported data: " + structuredData.getAccession());
          enaImportedData.add(data);
        }

        if (data.getDomain() == null && data.getWebinSubmissionAccountId() == null) {
          LOGGER.info("Found empty domain/webinId data: " + structuredData.getAccession());
          emptyDomainAndWebinData.add(data);
        }
      }

      for (StructuredDataTable data : emptyDomainAndWebinData) {
        StructuredDataTable duplicateWithDomain = StructuredDataTable.build("self.BiosampleImportNCBI",
            null, data.getType(), data.getSchema(), data.getContent());
        if (!dataSet.contains(duplicateWithDomain)) {
          StructuredDataTable dataWithDomain = StructuredDataTable.build("self.BiosampleImportNCBI",
              null, data.getType(), data.getSchema(), data.getContent());
          dataSet.add(dataWithDomain);
        }
      }

      dataSet.removeAll(enaImportedData);
      dataSet.removeAll(emptyDomainAndWebinData);

      LOGGER.info("Accession: " + structuredData.getAccession() + ", original size: " + originalSize + ", now: " + dataSet.size());
      mongoStructuredDataRepository.save(structuredData);

      return null;
    }

  }
}
