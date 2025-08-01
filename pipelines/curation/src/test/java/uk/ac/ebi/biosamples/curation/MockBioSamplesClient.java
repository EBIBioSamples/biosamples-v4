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
import uk.ac.ebi.biosamples.client.service.WebinAuthClientService;
import uk.ac.ebi.biosamples.client.utils.ClientProperties;
import uk.ac.ebi.biosamples.core.model.Curation;
import uk.ac.ebi.biosamples.core.model.CurationLink;
import uk.ac.ebi.biosamples.core.service.SampleValidator;

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
      final WebinAuthClientService webinAuthClientService,
      final ClientProperties clientProperties,
      final boolean logCurations) {
    super(
        uri, uriV2, restTemplateBuilder, sampleValidator, webinAuthClientService, clientProperties);

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
      final String accession, final Curation curation, final String webinId) {
    log.trace("Mocking persisting curation " + curation + " on " + accession + " in " + webinId);

    if (logCurations) {
      logCuration(accession, webinId, curation);
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
