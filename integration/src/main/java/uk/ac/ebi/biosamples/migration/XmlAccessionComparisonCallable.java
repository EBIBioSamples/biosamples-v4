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
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.service.XmlGroupToSampleConverter;
import uk.ac.ebi.biosamples.service.XmlSampleToSampleConverter;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

class XmlAccessionComparisonCallable implements Callable<Void> {
  private final RestTemplate restTemplate;
  private final String oldUrl;
  private final String newUrl;
  private final Queue<String> bothQueue;
  private final AtomicBoolean bothFlag;
  private final XmlSampleToSampleConverter xmlSampleToSampleConverter;
  private final XmlGroupToSampleConverter xmlGroupToSampleConverter;
  private final ExecutorService executorService = Executors.newFixedThreadPool(8);
  private final boolean compare;

  private final Logger log = LoggerFactory.getLogger(getClass());

  public XmlAccessionComparisonCallable(
      RestTemplate restTemplate,
      String oldUrl,
      String newUrl,
      Queue<String> bothQueue,
      AtomicBoolean bothFlag,
      XmlSampleToSampleConverter xmlSampleToSampleConverter,
      XmlGroupToSampleConverter xmlGroupToSampleConverter,
      boolean compare) {
    this.restTemplate = restTemplate;
    this.oldUrl = oldUrl;
    this.newUrl = newUrl;
    this.bothQueue = bothQueue;
    this.bothFlag = bothFlag;
    this.xmlSampleToSampleConverter = xmlSampleToSampleConverter;
    this.xmlGroupToSampleConverter = xmlGroupToSampleConverter;
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
        log.trace("Comparing accession " + accession);
        if (compare) {
          futures.put(
              accession,
              executorService.submit(
                  new XmlCompareCallable(
                      accession,
                      oldUrl,
                      newUrl,
                      xmlSampleToSampleConverter,
                      xmlGroupToSampleConverter,
                      restTemplate)));
          ThreadUtils.checkFutures(futures, 1000);
        }
      } else {
        Thread.sleep(100);
      }
    }
    ThreadUtils.checkFutures(futures, 0);
    executorService.awaitTermination(1, TimeUnit.MINUTES);
    log.info("Finished AccessionComparisonCallable.call(");
    return null;
  }
}
