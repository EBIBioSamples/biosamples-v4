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
package uk.ac.ebi.biosamples.curation;

import static org.mockito.Mockito.mock;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.hateoas.EntityModel;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.client.service.AapClientService;
import uk.ac.ebi.biosamples.client.utils.BioSamplesProperties;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.service.SampleValidator;

public class MockBioSamplesClient extends BioSamplesClient {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final Map<String, List<Curation>> curations = new HashMap<>();

  private final boolean logCurations;

  private PrintWriter printWriter;

  private FileWriter fileWriter;

  private int count = 0;

  MockBioSamplesClient(
      final URI uri,
      final URI uriV2,
      final RestTemplateBuilder restTemplateBuilder,
      final SampleValidator sampleValidator,
      final AapClientService aapClientService,
      final BioSamplesProperties bioSamplesProperties,
      final boolean logCurations) {
    super(uri, uriV2, restTemplateBuilder, sampleValidator, aapClientService, bioSamplesProperties);
    this.logCurations = logCurations;
    if (logCurations) {
      try {
        log.info("Logging curations");
        fileWriter = new FileWriter("curations.csv");
        printWriter = new PrintWriter(fileWriter);
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void finalize() {
    try {
      fileWriter.close();
      printWriter.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public EntityModel<CurationLink> persistCuration(
      final String accession,
      final Curation curation,
      final String webinIdOrDomain,
      final boolean isWebin) {
    log.trace(
        "Mocking persisting curation " + curation + " on " + accession + " in " + webinIdOrDomain);
    if (logCurations) {
      logCuration(accession, webinIdOrDomain, curation);
    }
    List<Curation> sampleCurations = curations.get(accession);
    if (sampleCurations == null) {
      sampleCurations = new ArrayList<>();
    }
    sampleCurations.add(curation);
    curations.put(accession, sampleCurations);
    return mock(EntityModel.class);
  }

  private String explainCuration(final Curation curation) {
    return StringEscapeUtils.escapeCsv(curation.toString());
  }

  private void logCuration(final String accession, final String domain, final Curation curation) {
    count++;
    printWriter.printf("%s,%s,%s\n", accession, domain, explainCuration(curation));
    if (count % 500 == 0) {
      log.info("Recorded " + count + " curations");
    }
  }

  Collection<Curation> getCurations(final String accession) {
    return curations.get(accession);
  }
}
