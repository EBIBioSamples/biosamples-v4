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

import java.io.StringReader;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

public class XmlAccessFetcherCallable implements Callable<Void> {

  private final RestTemplate restTemplate;
  private final String rootUrl;
  private final Queue<String> accessionQueue;
  private final AtomicBoolean finishFlag;
  private final boolean old;
  private final Logger log = LoggerFactory.getLogger(getClass());

  public XmlAccessFetcherCallable(
      final RestTemplate restTemplate,
      final String rootUrl,
      final Queue<String> accessionQueue,
      final AtomicBoolean finishFlag,
      final boolean old) {
    this.restTemplate = restTemplate;
    this.rootUrl = rootUrl;
    this.accessionQueue = accessionQueue;
    this.finishFlag = finishFlag;
    this.old = old;
  }

  @Override
  public Void call() throws Exception {
    log.info("Started against " + rootUrl);

    final long oldTime = System.nanoTime();
    final int pagesize = 1000;

    ExecutorService executorService = null;

    try {
      executorService = Executors.newFixedThreadPool(4);
      getPages("samples", pagesize, executorService, "");
      if (old) {
        getPages("groups", pagesize, executorService, "group");
      } else {
        getPages("groups", pagesize, executorService, "");
      }
    } finally {
      executorService.shutdownNow();
    }
    finishFlag.set(true);
    final long elapsed = System.nanoTime() - oldTime;
    log.info("Collected from " + rootUrl + " in " + (elapsed / 1000000000l) + "s");

    log.info("Finished AccessFetcherCallable.call(");

    return null;
  }

  private void getPages(
      final String pathSegment,
      final int pagesize,
      final ExecutorService executorService,
      final String query)
      throws DocumentException, InterruptedException, ExecutionException {

    final UriComponentsBuilder uriComponentBuilder =
        UriComponentsBuilder.fromUriString(rootUrl).pathSegment(pathSegment);
    uriComponentBuilder.replaceQueryParam("pagesize", pagesize);
    uriComponentBuilder.replaceQueryParam("query", query);

    // get the first page to get the number of pages in total
    uriComponentBuilder.replaceQueryParam("page", 1);
    final URI uri = uriComponentBuilder.build().toUri();

    final ResponseEntity<String> response;
    final RequestEntity<?> request = RequestEntity.get(uri).accept(MediaType.TEXT_XML).build();
    try {
      response = restTemplate.exchange(request, String.class);
    } catch (final RestClientException e) {
      log.error("Problem accessing " + uri, e);
      throw e;
    }
    final String xmlString = response.getBody();

    final SAXReader reader = new SAXReader();
    final Document xml = reader.read(new StringReader(xmlString));
    final Element root = xml.getRootElement();

    final int pageCount =
        (Integer.parseInt(XmlPathBuilder.of(root).path("SummaryInfo", "Total").text()) / pagesize)
            + 1;

    // multi-thread all the other pages via futures
    final List<Future<Set<String>>> futures = new ArrayList<>();
    for (int i = 1; i <= pageCount; i++) {
      uriComponentBuilder.replaceQueryParam("page", i);
      final URI pageUri = uriComponentBuilder.build().toUri();
      final Callable<Set<String>> callable = getPageCallable(pageUri);
      futures.add(executorService.submit(callable));
    }
    for (final Future<Set<String>> future : futures) {
      for (final String accession : future.get()) {
        while (!accessionQueue.offer(accession)) {
          Thread.sleep(10);
        }
      }
    }
  }

  private Callable<Set<String>> getPageCallable(final URI uri) {
    return new Callable<Set<String>>() {

      @Override
      public Set<String> call() throws Exception {
        final long startTime = System.nanoTime();
        final ResponseEntity<String> response;
        final RequestEntity<?> request = RequestEntity.get(uri).accept(MediaType.TEXT_XML).build();
        try {
          response = restTemplate.exchange(request, String.class);
        } catch (final RestClientException e) {
          log.error("Problem accessing " + uri, e);
          throw e;
        }
        final String xmlString = response.getBody();
        final long endTime = System.nanoTime();
        final long interval = (endTime - startTime) / 1000000l;
        log.info("Got " + uri + " in " + interval + "ms");

        final SAXReader reader = new SAXReader();
        final Document xml = reader.read(new StringReader(xmlString));
        final Element root = xml.getRootElement();

        final Set<String> accessions = new HashSet<>();

        for (final Element element : XmlPathBuilder.of(root).elements("BioSample")) {
          final String accession = element.attributeValue("id");
          // only handle sample accessions for now
          if (!accession.startsWith("SAMEG")) {
            accessions.add(accession);
          }
        }

        return accessions;
      }
    };
  }
}
