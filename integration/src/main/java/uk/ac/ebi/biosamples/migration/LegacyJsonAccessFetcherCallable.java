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
package uk.ac.ebi.biosamples.migration;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class LegacyJsonAccessFetcherCallable implements Callable<Void> {

  private final RestTemplate restTemplate;
  private final String rootUrl;
  private final Queue<String> accessionQueue;
  private final AtomicBoolean finishFlag;
  private final Logger log = LoggerFactory.getLogger(getClass());

  public LegacyJsonAccessFetcherCallable(
      RestTemplate restTemplate,
      String rootUrl,
      Queue<String> accessionQueue,
      AtomicBoolean finishFlag) {
    this.restTemplate = restTemplate;
    this.rootUrl = rootUrl;
    this.accessionQueue = accessionQueue;
    this.finishFlag = finishFlag;
  }

  @Override
  public Void call() throws Exception {
    log.info("Started against " + rootUrl);

    long oldTime = System.nanoTime();
    int pagesize = 1000;

    ExecutorService executorService = null;

    try {
      executorService = Executors.newFixedThreadPool(4);
      getPages("samples", pagesize, executorService);
      getPages("groups", pagesize, executorService);
    } finally {
      executorService.shutdown();
      executorService.awaitTermination(1, TimeUnit.DAYS);
    }
    finishFlag.set(true);
    long elapsed = System.nanoTime() - oldTime;
    log.info("Collected from " + rootUrl + " in " + (elapsed / 1000000000l) + "s");

    log.info("Finished AccessFetcherCallable.call(");

    return null;
  }

  private void getPages(String pathSegment, int pagesize, ExecutorService executorService)
      throws DocumentException, InterruptedException, ExecutionException {

    UriComponentsBuilder uriComponentBuilder =
        UriComponentsBuilder.fromUriString(rootUrl).pathSegment(pathSegment);
    uriComponentBuilder.replaceQueryParam("size", pagesize);

    // get the first page to get the number of pages in total
    //		uriComponentBuilder.replaceQueryParam("page", 1);
    URI uri = uriComponentBuilder.build().toUri();

    ResponseEntity<String> response;
    RequestEntity<?> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
    try {
      response = restTemplate.exchange(request, String.class);
    } catch (RestClientException e) {
      log.error("Problem accessing " + uri, e);
      throw e;
    }
    String jsonString = response.getBody();

    int pageCount = JsonPath.read(jsonString, "$.page.totalPages");

    // multi-thread all the other pages via futures
    List<Future<Set<String>>> futures = new ArrayList<>();
    for (int i = 0; i <= pageCount; i++) {
      uriComponentBuilder.replaceQueryParam("page", i);
      URI pageUri = uriComponentBuilder.build().toUri();
      Callable<Set<String>> callable = getPageCallable(pageUri);
      futures.add(executorService.submit(callable));
    }
    for (Future<Set<String>> future : futures) {
      for (String accession : future.get()) {
        while (!accessionQueue.offer(accession)) {
          Thread.sleep(10);
        }
      }
    }
  }

  public Callable<Set<String>> getPageCallable(URI uri) {
    return new Callable<Set<String>>() {

      @Override
      public Set<String> call() throws Exception {
        long startTime = System.nanoTime();
        ResponseEntity<String> response;
        RequestEntity<?> request = RequestEntity.get(uri).accept(MediaTypes.HAL_JSON).build();
        try {
          response = restTemplate.exchange(request, String.class);
        } catch (RestClientException e) {
          log.error("Problem accessing " + uri, e);
          throw e;
        }
        String jsonString = response.getBody();
        long endTime = System.nanoTime();
        long interval = (endTime - startTime) / 1000000l;
        log.info("Got " + uri + " in " + interval + "ms");

        String jsonPathType = "samples";
        if (uri.getPath().contains("groups")) {
          jsonPathType = "groups";
        }

        List<String> accessions =
            JsonPath.read(jsonString, "$._embedded." + jsonPathType + ".*.accession");
        return new HashSet<>(accessions);
      }
    };
  }
}
