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
package uk.ac.ebi.biosamples.ena.amr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ena.amr.service.  EnaAmrDataProcessService;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.utils.AdaptiveThreadPoolExecutor;
import uk.ac.ebi.biosamples.utils.MailSender;
import uk.ac.ebi.biosamples.utils.ThreadUtils;

@Component
public class AmrRunner implements ApplicationRunner {
  public static final String SAMEA = "SAMEA";
  private static final String BSD_SAMPLE_PREFIX = "SA";
  private static final String FTP = "ftp";
  private static final String MD_5 = "md5";
  private static final String HTTP = "http://";
  private static final String antibiogram = "\"AMR_ANTIBIOGRAM\"";
  private static final String URL =
      "https://www.ebi.ac.uk/ena/portal/api/search?result=analysis&query=analysis_type="
          + antibiogram
          + "&dataPortal=pathogen&dccDataOnly=false&fields=analysis_accession,country,region,scientific_name,location,sample_accession,tax_id,submitted_ftp,first_public,last_updated&sortFields=scientific_name,country&limit=0";
  private static final Logger log = LoggerFactory.getLogger(AmrRunner.class);

  public static final String TAB = "\t";

  @Autowired EnaAmrDataProcessService enaAmrDataProcessService;
  @Autowired
  @Qualifier("WEBINCLIENT")
  BioSamplesClient bioSamplesClient;

  @Override
  public void run(final ApplicationArguments args) {
    log.info("Processing ENA-AMR pipeline");

    List<AccessionFtpUrlPair> pairList;
    boolean isPipelineOk = true;

    try {
      pairList = requestHttpAndGetAccessionFtpUrlPairs();

      if (pairList.size() == 0) {
        log.info(
            "Unable to fetch ENA-AMR Antibiogram data from ENA API, Timed out waiting for connection");
        isPipelineOk = false;
      }

      downloadFtpContent(pairList);
    } catch (Exception e) {
      log.info("An exception occured while processing AMR data " + e.getMessage());
      isPipelineOk = false;
    } finally {
      final StringBuffer failedFiles = new StringBuffer();

      EnaAmrDataProcessService.failedQueue.forEach(
          failedAccession -> {
            failedFiles.append(failedAccession);
            failedFiles.append(",");
          });

      log.info(
          EnaAmrDataProcessService.failedQueue.size()
              + " failed files: accessions are "
              + failedFiles.toString());

      MailSender.sendEmail("ENA-AMR", failedFiles.toString(), isPipelineOk);
    }
  }

  private static List<AccessionFtpUrlPair> requestHttpAndGetAccessionFtpUrlPairs()
      throws Exception {
    final URL enaApiUrl = new URL(AmrRunner.URL);
    final HttpURLConnection conn = (HttpURLConnection) enaApiUrl.openConnection();
    List<AccessionFtpUrlPair> pairList = new ArrayList<>();

    try {
      if (getResponseFromEnaApi(conn) == 200) {
        pairList = doGetAccessionFtpUrlPairs(enaApiUrl);
      }
    } catch (final Exception e) {
      throw new RuntimeException(e);
    } finally {
      conn.disconnect();
    }

    return pairList;
  }

  private static int getResponseFromEnaApi(final HttpURLConnection conn) throws IOException {
    int response;

    conn.setRequestMethod("GET");
    conn.connect();
    response = conn.getResponseCode();

    return response;
  }

  private static List<AccessionFtpUrlPair> doGetAccessionFtpUrlPairs(final URL url) {
    final List<AccessionFtpUrlPair> accessionFtpUrlPairs = new ArrayList<>();

    try {
      BufferedReader bufferedReader = getReader(url);

      bufferedReader
          .lines()
          .forEach(line -> accessionFtpUrlPairs.add(getAccessionFtpUrlPair(line)));
    } catch (final IOException e) {
      log.info("Failed to get and parse accession and FTP pairs for URL " + url.toString());
    }

    return accessionFtpUrlPairs;
  }

  private static BufferedReader getReader(final URL url) throws IOException {
    return new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
  }

  private static AccessionFtpUrlPair getAccessionFtpUrlPair(final String line) {
    final StringTokenizer tokenizer = new StringTokenizer(line, TAB);
    final AccessionFtpUrlPair accessionFtpUrlPair = new AccessionFtpUrlPair();

    while (tokenizer.hasMoreTokens()) {
      final String value = tokenizer.nextToken();

      if (value.startsWith(BSD_SAMPLE_PREFIX)) {
        accessionFtpUrlPair.setAccession(value);
      }

      if (value.startsWith(FTP)) {
        dealWithSemicolon(value, accessionFtpUrlPair);
      }
    }

    return accessionFtpUrlPair;
  }

  private static void dealWithSemicolon(
      final String value, final AccessionFtpUrlPair accessionFtpUrlPair) {
    // Commented because ENA's storing storing representation of AMR files has changed, no parsing required
    /*final int index = value.indexOf(';');
    final String option1 = value.substring(index + 1);
    final String option2 = value.substring(0, index);

    if (!option1.endsWith(MD_5)) {
      accessionFtpUrlPair.setFtpUrl(HTTP + option1);
    } else {
      accessionFtpUrlPair.setFtpUrl(HTTP + option2);
    }*/

    accessionFtpUrlPair.setFtpUrl(HTTP + value);
  }

  private void downloadFtpContent(final List<AccessionFtpUrlPair> pairList) {
    try (final AdaptiveThreadPoolExecutor executorService =
        AdaptiveThreadPoolExecutor.create(100, 10000, true, 1, 10)) {
      Map<String, Future<Void>> futures = new HashMap<>();

      pairList.forEach(
          pair -> {
            try {
              String accession = pair.getAccession();

              if (accession != null)
                futures.put(
                    accession,
                    executorService.submit(
                        new EnaAmrCallable(new URL(pair.getFtpUrl()), accession)));
            } catch (MalformedURLException e) {
              log.info("FTP URL not correctly formed " + pair.getFtpUrl());
            }
          });

      log.info("waiting for futures");
      // wait for anything to finish
      ThreadUtils.checkFutures(futures, 0);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }

  class EnaAmrCallable implements Callable<Void> {
    URL url;
    String accession;

    EnaAmrCallable(final URL url, final String accession) {
      this.url = url;
      this.accession = accession;
    }

    @Override
    public Void call() {
      return fetchSampleAndProcessAmrData(url, accession);
    }

    private Void fetchSampleAndProcessAmrData(final URL url, final String accession) {
      final List<String> curationDomainBlankList = new ArrayList<>();
      curationDomainBlankList.add("");

      try {
        final Optional<EntityModel<Sample>> sample =
            bioSamplesClient.fetchSampleResource(accession, Optional.of(curationDomainBlankList));

        if (sample.isPresent()) {
          enaAmrDataProcessService.processAmrData(
              enaAmrDataProcessService.processAmrLines(getReader(url)),
              sample.get().getContent(),
              bioSamplesClient);
        } else {
          log.info(accession + " doesn't exist");
        }
      } catch (final IOException ioe) {
        EnaAmrDataProcessService.failedQueue.add(accession);
        log.info("Couldn't process AMR data for " + accession);
      }

      return null;
    }
  }
}
