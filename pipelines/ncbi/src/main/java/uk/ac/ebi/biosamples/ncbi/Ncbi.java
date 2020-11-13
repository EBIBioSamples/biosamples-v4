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
package uk.ac.ebi.biosamples.ncbi;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.PipelinesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.AbstractData;
import uk.ac.ebi.biosamples.service.AmrDataLoaderService;
import uk.ac.ebi.biosamples.service.FilterBuilder;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.MailSender;
import uk.ac.ebi.biosamples.utils.ThreadUtils;
import uk.ac.ebi.biosamples.utils.XmlFragmenter;

@Component
@Profile("!test")
public class Ncbi implements ApplicationRunner {
  private Logger log = LoggerFactory.getLogger(getClass());
  private final PipelinesProperties pipelinesProperties;
  private final XmlFragmenter xmlFragmenter;
  private final NcbiFragmentCallback sampleCallback;
  private final BioSamplesClient bioSamplesClient;

  @Autowired private AmrDataLoaderService amrDataLoaderService;

  private Map<String, Set<AbstractData>> sampleToAmrMap = new HashMap<>();

  public Ncbi(
      PipelinesProperties pipelinesProperties,
      XmlFragmenter xmlFragmenter,
      NcbiFragmentCallback sampleCallback,
      BioSamplesClient bioSamplesClient) {
    this.pipelinesProperties = pipelinesProperties;
    this.xmlFragmenter = xmlFragmenter;
    this.sampleCallback = sampleCallback;
    this.bioSamplesClient = bioSamplesClient;
  }

  @Override
  public void run(ApplicationArguments args) {
    boolean isPassed = true;
    boolean includeAmr = true;

    if (args.getOptionNames().contains("includeAmr")) {
      if (args.getOptionValues("includeAmr").iterator().next().equalsIgnoreCase("false")) {
        includeAmr = false;
      }
    }

    if (includeAmr && isFirstDayOfTheWeek()) {
      try {
        sampleToAmrMap = amrDataLoaderService.loadAmrData();
      } catch (final Exception e) {
        log.error("Error in processing AMR data from ENA API - continue with the pipeline");
      }
    }

    try {
      log.info("Processing NCBI pipeline...");

      LocalDate fromDate;
      if (args.getOptionNames().contains("from")) {
        fromDate =
            LocalDate.parse(
                args.getOptionValues("from").iterator().next(), DateTimeFormatter.ISO_LOCAL_DATE);
      } else {
        fromDate = LocalDate.parse("1000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
      }
      LocalDate toDate = null;
      if (args.getOptionNames().contains("until")) {
        toDate =
            LocalDate.parse(
                args.getOptionValues("until").iterator().next(), DateTimeFormatter.ISO_LOCAL_DATE);
      } else {
        toDate = LocalDate.parse("3000-01-01", DateTimeFormatter.ISO_LOCAL_DATE);
      }

      log.info("Processing samples from " + DateTimeFormatter.ISO_LOCAL_DATE.format(fromDate));
      log.info("Processing samples to " + DateTimeFormatter.ISO_LOCAL_DATE.format(toDate));
      sampleCallback.setFromDate(fromDate);
      sampleCallback.setToDate(toDate);

      String ncbiFile;
      if (args.getOptionNames().contains("ncbi_file")) {
        ncbiFile = args.getOptionValues("ncbi_file").get(0);
      } else {
        ncbiFile = "C:\\Users\\dgupta\\ncbi_test\\biosample_result.xml.gz";
      }

      Path inputPath = Paths.get(ncbiFile);
      inputPath = inputPath.toAbsolutePath();

      try (InputStream is =
          new GZIPInputStream(new BufferedInputStream(Files.newInputStream(inputPath)))) {
        if (pipelinesProperties.getThreadCount() > 0) {
          ExecutorService executorService = null;
          try {
            executorService =
                AdaptiveThreadPoolExecutor.create(
                    100,
                    10000,
                    true,
                    pipelinesProperties.getThreadCount(),
                    pipelinesProperties.getThreadCountMax());
            Map<Element, Future<Void>> futures = new LinkedHashMap<>();

            sampleCallback.setExecutorService(executorService);
            sampleCallback.setFutures(futures);
            sampleCallback.setSampleToAmrMap(sampleToAmrMap);

            // this does the actual processing
            xmlFragmenter.handleStream(is, "UTF-8", sampleCallback);

            log.info("waiting for futures");

            // wait for anything to finish
            ThreadUtils.checkFutures(futures, 0);
          } finally {
            log.info("shutting down");
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.MINUTES);
          }
        } else {
          // do all on master thread
          // this does the actual processing
          xmlFragmenter.handleStream(is, "UTF-8", sampleCallback);
        }
      }
      log.info("Handled new and updated NCBI samples");
      log.info("Number of accession from NCBI = " + sampleCallback.getAccessions().size());
      // remove old NCBI samples no longer present
      // get all existing NCBI samples
      makingNcbiSamplesPrivate();
      log.info("Processed NCBI pipeline");
    } catch (final Exception e) {
      log.error("Pipeline failed to finish successfully", e);
      isPassed = false;
    } finally {
      MailSender.sendEmail("NCBI", null, isPassed);
    }
  }

  private void makingNcbiSamplesPrivate() {
    // Run every Monday as this scans through all samples, not required to run each day
    if (isFirstDayOfTheWeek()) {
      Set<String> toRemoveAccessions = getExistingPublicNcbiAccessions();
      // remove those that still exist
      toRemoveAccessions.removeAll(sampleCallback.getAccessions());
      // remove those samples that are left
      log.info("Number of samples to make private = " + toRemoveAccessions.size());
      makePrivate(toRemoveAccessions);
    }
  }

  private boolean isFirstDayOfTheWeek() {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());

    return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY;
  }

  private Set<String> getExistingPublicNcbiAccessions() {
    try {
      log.info("getting existing public ncbi accessions");
      long startTime = System.nanoTime();
      // make sure to only get the public samples
      Set<String> existingAccessions = new TreeSet<>();
      for (Resource<Sample> sample :
          bioSamplesClient
              .getPublicClient()
              .get()
              .fetchSampleResourceAll(
                  Collections.singleton(FilterBuilder.create().onAccession("SAM[^E].*").build()))) {
        existingAccessions.add(sample.getContent().getAccession());
      }
      long endTime = System.nanoTime();
      double intervalSec = ((double) (endTime - startTime)) / 1000000000.0;
      log.debug(
          "Took "
              + intervalSec
              + "s to get "
              + existingAccessions.size()
              + " existing public ncbi accessions");
      return existingAccessions;
    } catch (final Exception publicNcbiAccessionFetchException) {
      throw new RuntimeException(publicNcbiAccessionFetchException);
    }
  }

  private void makePrivate(Set<String> toRemoveAccessions) {
    // TODO make this multithreaded for performance
    final List<String> curationDomainBlankList = new ArrayList<>();
    curationDomainBlankList.add("");

    try {
      for (String accession : toRemoveAccessions) {
        // this must get the ORIGINAL sample without curation
        Optional<Resource<Sample>> sampleOptional =
            bioSamplesClient.fetchSampleResource(accession, Optional.of(curationDomainBlankList));
        if (sampleOptional.isPresent()) {
          Sample sample = sampleOptional.get().getContent();
          // set the release date to 1000 years in the future to make it private again
          // remove structured data if any
          Sample newSample =
              Sample.Builder.fromSample(sample)
                  .withNoData()
                  .withRelease(ZonedDateTime.now(ZoneOffset.UTC).plusYears(1000).toInstant())
                  .build();
          // persist the now private sample
          log.info("Making private " + sample.getAccession());
          bioSamplesClient.persistSampleResource(newSample);
        }
      }
    } catch (final Exception sampleMakePrivateException) {
      log.info(
          "HTTP Exception while making sample private " + sampleMakePrivateException.getCause());
    }
  }
}
