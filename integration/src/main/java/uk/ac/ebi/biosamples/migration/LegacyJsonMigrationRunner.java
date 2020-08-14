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
package uk.ac.ebi.biosamples.migration;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.legacy.json.service.JSONSampleToSampleConverter;

// @Component
@Profile({"migration-json"})
public class LegacyJsonMigrationRunner implements ApplicationRunner, ExitCodeGenerator {

  private final RestTemplate restTemplate;
  private ExecutorService executorService;

  private int exitCode = 1;
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Value("${biosamples.migration.old:http://www.ebi.ac.uk/biosamples/xml/samples}")
  private String oldUrl;

  @Value("${biosamples.migration.new:http://www.ebi.ac.uk/biosamples/xml/samples}")
  private String newUrl;

  private final JSONSampleToSampleConverter legacyJSONConverter;

  public LegacyJsonMigrationRunner(
      RestTemplateBuilder restTemplateBuilder, JSONSampleToSampleConverter legacyJsonConverter) {
    restTemplate = restTemplateBuilder.build();
    legacyJSONConverter = legacyJsonConverter;
  }

  @Override
  public int getExitCode() {
    return exitCode;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    log.info("Starting MigrationRunner");

    //		if (args.containsOption("test")) {
    //			Queue<String> testAccession = new ArrayBlockingQueue<String>(1);
    //			testAccession.add("SAMEA4341168");
    //			AtomicBoolean bothQueues = new AtomicBoolean(true);
    //			LegacyJsonAccessionComparisonCallable callable = new
    // LegacyJsonAccessionComparisonCallable(restTemplate,
    //					oldUrl, newUrl, testAccession, bothQueues, legacyJSONConverter,
    // args.containsOption("comparison"));
    //			callable.call();
    //		} else {

    try {
      executorService = Executors.newFixedThreadPool(4);
      Queue<String> oldQueue = new ArrayBlockingQueue<>(1024);
      AtomicBoolean oldFinished = new AtomicBoolean(false);
      LegacyJsonAccessFetcherCallable oldCallable =
          new LegacyJsonAccessFetcherCallable(restTemplate, oldUrl, oldQueue, oldFinished);

      Queue<String> newQueue = new ArrayBlockingQueue<>(1024);
      AtomicBoolean newFinished = new AtomicBoolean(false);
      LegacyJsonAccessFetcherCallable newCallable =
          new LegacyJsonAccessFetcherCallable(restTemplate, newUrl, newQueue, newFinished);

      Queue<String> bothQueue = new ArrayBlockingQueue<>(1024);
      AtomicBoolean bothFinished = new AtomicBoolean(false);

      AccessionQueueBothCallable bothCallable =
          new AccessionQueueBothCallable(
              oldQueue, oldFinished, newQueue, newFinished, bothQueue, bothFinished);

      LegacyJsonAccessionComparisonCallable comparisonCallable =
          new LegacyJsonAccessionComparisonCallable(
              restTemplate, oldUrl, newUrl, bothQueue, bothFinished, legacyJSONConverter, true);

      //			comparisonCallable.compare("SAMEA3683023");

      //				CompletableFuture<Void> retriveAccessionFuture = new CompletableFuture.allOf(
      //						CompletableFuture.run(oldCallable, executorService),
      // executorService.submit(newCallable)
      //				);
      Future<Void> oldFuture = executorService.submit(oldCallable);
      Future<Void> newFuture = executorService.submit(newCallable);
      Future<Void> bothFuture = executorService.submit(bothCallable);
      Future<Void> comparisonFuture = executorService.submit(comparisonCallable);

      oldFuture.get();
      newFuture.get();
      bothFuture.get();
      comparisonFuture.get();
    } finally {
      executorService.shutdown();
      executorService.awaitTermination(1, TimeUnit.MINUTES);
    }
    //		}

    exitCode = 0;
    log.info("Finished MigrationRunner");
  }
}
