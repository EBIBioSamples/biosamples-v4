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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;
import uk.ac.ebi.biosamples.mongo.model.MongoStructuredData;
import uk.ac.ebi.biosamples.mongo.repo.MongoStructuredDataRepository;
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
public class AmrCleanRunner implements ApplicationRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRunner.class);
  private final MongoStructuredDataRepository mongoStructuredDataRepository;
  private final MongoOperations mongoOperations;

  @Autowired
  public AmrCleanRunner(
      final MongoStructuredDataRepository mongoStructuredDataRepository,
      final MongoOperations mongoOperations) {
    this.mongoStructuredDataRepository = mongoStructuredDataRepository;
    this.mongoOperations = mongoOperations;
  }

  @Override
  public void run(final ApplicationArguments args) throws Exception {
    final Map<String, Future<Void>> futures = new HashMap<>();
    ExecutorService executor = null;

    try {
      executor = Executors.newFixedThreadPool(128);

      final Query query = new Query();
      try (final CloseableIterator<MongoStructuredData> it =
          mongoOperations.stream(query, MongoStructuredData.class)) {
        while (it.hasNext()) {
          final MongoStructuredData structuredData = it.next();
          final String accession = structuredData.getAccession();
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

    SdCleanCallable(
        final MongoStructuredData structuredData,
        final MongoStructuredDataRepository mongoStructuredDataRepository) {
      this.structuredData = structuredData;
      this.mongoStructuredDataRepository = mongoStructuredDataRepository;
    }

    @Override
    public Void call() {
      final Set<StructuredDataTable> dataSet = structuredData.getData();
      final Set<StructuredDataTable> enaImportedData = new HashSet<>();
      final Set<StructuredDataTable> emptyDomainAndWebinData = new HashSet<>();
      final int originalSize = dataSet.size();

      for (final StructuredDataTable data : dataSet) {
        if (!data.getType().equalsIgnoreCase("AMR")) {
          continue;
        }

        if ("Webin-40894".equals(data.getWebinSubmissionAccountId())) {
          LOGGER.info("Found ENA imported data: " + structuredData.getAccession());
          enaImportedData.add(data);
        }

        if (data.getDomain() == null && data.getWebinSubmissionAccountId() == null) {
          LOGGER.info("Found empty domain/webinId data: " + structuredData.getAccession());
          emptyDomainAndWebinData.add(data);
        }
      }

      for (final StructuredDataTable data : emptyDomainAndWebinData) {
        final StructuredDataTable duplicateWithDomain =
            StructuredDataTable.build(
                "self.BiosampleImportNCBI",
                null,
                data.getType(),
                data.getSchema(),
                data.getContent());
        if (!dataSet.contains(duplicateWithDomain)) {
          final StructuredDataTable dataWithDomain =
              StructuredDataTable.build(
                  "self.BiosampleImportNCBI",
                  null,
                  data.getType(),
                  data.getSchema(),
                  data.getContent());
          dataSet.add(dataWithDomain);
        }
      }

      dataSet.removeAll(enaImportedData);
      dataSet.removeAll(emptyDomainAndWebinData);

      LOGGER.info(
          structuredData.getAccession()
              + ", original size: "
              + originalSize
              + ", now: "
              + dataSet.size());
      mongoStructuredDataRepository.save(structuredData);

      return null;
    }
  }
}
