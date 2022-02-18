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
package uk.ac.ebi.biosamples.ncbi;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;
import uk.ac.ebi.biosamples.utils.ThreadUtils;
import uk.ac.ebi.biosamples.utils.XmlFragmenter.ElementCallback;

@Component
public class NcbiFragmentCallback implements ElementCallback {
  private Logger log = LoggerFactory.getLogger(getClass());
  private final NcbiElementCallableFactory ncbiElementCallableFactory;
  private final PipelinesProperties pipelinesProperties;
  private final SortedSet<String> accessions = new TreeSet<>();
  private LocalDate fromDate;
  private LocalDate toDate;
  private ExecutorService executorService;
  private Map<Element, Future<Void>> futures;
  private Map<String, Set<StructuredDataTable>> sampleToAmrMap = new HashMap<>();

  private NcbiFragmentCallback(
      NcbiElementCallableFactory ncbiElementCallableFactory,
      PipelinesProperties pipelinesProperties) {
    this.ncbiElementCallableFactory = ncbiElementCallableFactory;
    this.pipelinesProperties = pipelinesProperties;
  }

  public LocalDate getFromDate() {
    return fromDate;
  }

  public void setFromDate(LocalDate fromDate) {
    this.fromDate = fromDate;
  }

  public LocalDate getToDate() {
    return toDate;
  }

  public void setToDate(LocalDate toDate) {
    this.toDate = toDate;
  }

  public ExecutorService getExecutorService() {
    return executorService;
  }

  public void setExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
  }

  public Map<Element, Future<Void>> getFutures() {
    return futures;
  }

  public void setFutures(Map<Element, Future<Void>> futures) {
    this.futures = futures;
  }

  public void setSampleToAmrMap(Map<String, Set<StructuredDataTable>> sampleToAmrMap) {
    this.sampleToAmrMap = sampleToAmrMap;
  }

  public SortedSet<String> getAccessions() {
    return Collections.unmodifiableSortedSet(accessions);
  }

  @Override
  public void handleElement(Element element) throws InterruptedException, ExecutionException {
    log.trace("Handling element");

    Callable<Void> callable = ncbiElementCallableFactory.build(element, sampleToAmrMap);

    if (executorService == null) {
      try {
        callable.call();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else {
      Future<Void> future = executorService.submit(callable);
      if (futures != null) {
        futures.put(element, future);
      }
      ThreadUtils.checkFutures(futures, 100);
    }
  }

  @Override
  public boolean isBlockStart(String uri, String localName, String qName, Attributes attributes) {
    // its not a biosample element, skip
    if (!qName.equals("BioSample")) {
      return false;
    }
    // its not public, skip
    if (attributes.getValue("", "access").equals("public")) {
      // do nothing
    } else if (pipelinesProperties.getNcbiControlledAccess()
        && attributes.getValue("", "access").equals("controlled-access")) {
      // do nothing
    } else {
      return false;
    }
    // its an EBI biosample, or has no accession, skip
    String accession = attributes.getValue("", "accession");
    if (accession == null || accession.startsWith("SAME")) {
      return false;
    }

    // at this point, we know its a sensible accession, store it
    accessions.add(accession);

    // check the date compared to window
    LocalDate updateDate = null;
    if (attributes.getValue("", "last_update") != null) {
      updateDate =
          LocalDate.parse(
              attributes.getValue("", "last_update"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    } else {
      // no update date, abort
      return false;
    }
    LocalDate releaseDate = null;
    if (attributes.getValue("", "publication_date") != null) {
      releaseDate =
          LocalDate.parse(
              attributes.getValue("", "publication_date"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    } else {
      // no release date, abort
      return false;
    }

    LocalDate latestDate = updateDate;
    if (releaseDate.isAfter(latestDate)) {
      latestDate = releaseDate;
    }

    if (fromDate != null && latestDate.isBefore(fromDate)) {
      return false;
    }

    if (toDate != null && latestDate.isAfter(toDate)) {
      return false;
    }

    // hasn't failed, so we must be interested in it
    return true;
  }
}
