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

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.legacy.json.service.JSONSampleToSampleConverter;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

class LegacyJsonAccessionComparisonCallable implements Callable<Void> {
  private final RestTemplate restTemplate;
  private final String oldUrl;
  private final String newUrl;
  private final Queue<String> bothQueue;
  private final AtomicBoolean bothFlag;
  private final JSONSampleToSampleConverter legacyJsonConverter;
  private final ExecutorService executorService = Executors.newFixedThreadPool(8);
  private final boolean compare;

  private final Logger log = LoggerFactory.getLogger(getClass());

  public LegacyJsonAccessionComparisonCallable(
      RestTemplate restTemplate,
      String oldUrl,
      String newUrl,
      Queue<String> bothQueue,
      AtomicBoolean bothFlag,
      JSONSampleToSampleConverter legacyJsonConverter,
      boolean compare) {
    this.restTemplate = restTemplate;
    this.oldUrl = oldUrl;
    this.newUrl = newUrl;
    this.bothQueue = bothQueue;
    this.bothFlag = bothFlag;
    this.legacyJsonConverter = legacyJsonConverter;
    this.compare = compare;
  }

  @Override
  public Void call() throws Exception {
    log.info("Started");
    log.info("oldUrl = " + oldUrl);
    log.info("newUrl = " + newUrl);
    log.info("compare = " + compare);
    Map<String, Future<Void>> futures = new LinkedHashMap<>();

    while (!bothFlag.get() || !bothQueue.isEmpty()) {
      String accession = bothQueue.poll();
      if (accession != null) {
        log.info("Comparing accession " + accession);
        if (compare) {
          futures.put(
              accession,
              executorService.submit(
                  new JsonCompareCallable(
                      accession, oldUrl, newUrl, legacyJsonConverter, restTemplate)));
          ThreadUtils.checkFutures(futures, 1000);
        }
      } else {
        Thread.sleep(100);
        //				log.info("Waiting while accession is null");
      }
    }
    ThreadUtils.checkFutures(futures, 0);
    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.MINUTES);
    log.info("Finished AccessionComparisonCallable.call(");
    return null;
  }

  private static class JsonCompareCallable implements Callable<Void> {
    private final String accession;
    private final String oldUrl;
    private final String newUrl;
    private final RestTemplate restTemplate;
    private final JSONSampleToSampleConverter legacyJsonConverter;
    private final Comparer comparer = new Comparer();
    private final Logger log = LoggerFactory.getLogger(getClass());

    public JsonCompareCallable(
        String accession,
        String oldUrl,
        String newUrl,
        JSONSampleToSampleConverter legacyJsonConverter,
        RestTemplate restTemplate) {
      this.accession = accession;
      this.oldUrl = oldUrl;
      this.newUrl = newUrl;
      this.legacyJsonConverter = legacyJsonConverter;
      this.restTemplate = restTemplate;
    }

    @Override
    public Void call() throws Exception {

      UriComponentsBuilder oldUriComponentBuilder = UriComponentsBuilder.fromUriString(oldUrl);
      UriComponentsBuilder newUriComponentBuilder = UriComponentsBuilder.fromUriString(newUrl);

      String endpoint = accession.startsWith("SAMEG") ? "groups" : "samples";

      URI oldUri =
          oldUriComponentBuilder.cloneBuilder().pathSegment(endpoint, accession).build().toUri();
      URI newUri =
          newUriComponentBuilder.cloneBuilder().pathSegment(endpoint, accession).build().toUri();
      String oldDocument = getDocument(oldUri);
      String newDocument = getDocument(newUri);

      Sample newSample = legacyJsonConverter.convert(newDocument);
      Sample oldSample = legacyJsonConverter.convert(oldDocument);

      comparer.compare(accession, oldSample, newSample);
      return null;
    }

    private String getDocument(URI uri) {
      // log.info("Getting " + uri);
      ResponseEntity<String> response;
      try {
        response = restTemplate.getForEntity(uri, String.class);
      } catch (RestClientException e) {
        log.error("Problem accessing " + uri, e);
        throw e;
      }
      return response.getBody();
    }
  }
}
