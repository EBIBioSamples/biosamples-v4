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
package uk.ac.ebi.biosamples;

import java.time.Instant;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.utils.IntegrationTestFailException;

@Component
@Order(2)
public class JsonSchemaValidationIntegration extends AbstractIntegration {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final BioSamplesClient webinTestClient;
  private final BioSamplesClient webinClient;

  public JsonSchemaValidationIntegration(
      final BioSamplesClient client,
      @Qualifier("WEBINTESTCLIENT") final BioSamplesClient webinTestClient,
      final BioSamplesClient webinClient) {
    super(client);

    this.webinTestClient = webinTestClient;
    this.webinClient = webinClient;
  }

  @Override
  protected void phaseOne() {}

  @Override
  protected void phaseTwo() throws InterruptedException {}

  @Override
  protected void phaseThree() throws InterruptedException {}

  @Override
  protected void phaseFour() {}

  @Override
  protected void phaseFive() {}

  @Override
  protected void phaseSix() throws ExecutionException, InterruptedException {
    try {
      log.info("Starting JSON schema validation related tests");

      webinTestClient.persistSampleResource(getSample());
    } catch (final Exception e) {
      final HttpClientErrorException httpClientErrorException = (HttpClientErrorException) e;
      final String message = httpClientErrorException.getMessage();

      if (Objects.requireNonNull(message)
          .contains("Validation of sample metadata against this schema is not permitted")) {
        log.info("Expectedly failed with message " + message);
      } else {
        throw new IntegrationTestFailException(
            "Actual and expected error doesn't match for user trying to use ERC000011:0.1 schema for sample submission",
            Phase.SIX);
      }
    }

    final Sample sample = webinClient.persistSampleResource(getSample()).getContent();

    if (sample == null) {
      throw new IntegrationTestFailException(
          "Super user should be able to use ERC000011:0.1 schema for sample submission", Phase.SIX);
    }
  }

  private Sample getSample() {
    final String name = "Sample_1";
    final Instant update = Instant.parse("2025-01-01T11:36:57.00Z");
    final Instant release = Instant.parse("2025-01-01T11:36:57.00Z");
    final SortedSet<Attribute> attributes = new TreeSet<>();

    attributes.add(
        Attribute.build(
            "organism", "Homo sapiens", "http://purl.obolibrary.org/obo/NCBITaxon_9606", null));
    attributes.add(Attribute.build("checklist", "ERC000011:0.1"));

    return new Sample.Builder(name)
        .withUpdate(update)
        .withRelease(release)
        .withWebinSubmissionAccountId("Webin-40894")
        .withAttributes(attributes)
        .build();
  }
}
