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
package uk.ac.ebi.biosamples.ena;

import java.util.*;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ega.EgaSampleExporter;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.EnaSampleToBioSampleConversionService;

public class EnaImportCallable implements Callable<Void> {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private static final int MAX_RETRIES = 5;
  private final String accession;
  private final String egaId;
  private final BioSamplesClient bioSamplesWebinClient;
  private final EgaSampleExporter egaSampleExporter;
  private final EnaSampleToBioSampleConversionService enaSampleToBioSampleConversionService;

  private final boolean suppressionRunner;

  EnaImportCallable(
      final String accession,
      final String egaId,
      final BioSamplesClient bioSamplesWebinClient,
      final EgaSampleExporter egaSampleExporter,
      final EnaSampleToBioSampleConversionService enaSampleToBioSampleConversionService,
      final boolean suppressionRunner) {
    this.accession = accession;
    this.egaId = egaId;
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.egaSampleExporter = egaSampleExporter;
    this.enaSampleToBioSampleConversionService = enaSampleToBioSampleConversionService;
    this.suppressionRunner = suppressionRunner;
  }

  @Override
  public Void call() {
    final List<String> curationDomainBlankList = new ArrayList<>();
    curationDomainBlankList.add("");

    if (suppressionRunner) {
      final Optional<EntityModel<Sample>> sampleOptionalInBioSamples =
          bioSamplesWebinClient.fetchSampleResource(
              accession, Optional.of(curationDomainBlankList));
      final Sample sampleInBioSamples =
          sampleOptionalInBioSamples.map(EntityModel::getContent).orElse(null);

      if (sampleInBioSamples != null) {
        final Set<Attribute> sampleAttributes = sampleInBioSamples.getAttributes();
        final Optional<Attribute> insdcStatusAttributeOptional =
            sampleAttributes.stream()
                .filter(attribute -> attribute.getType().equals("INSDC Status"))
                .findFirst();

        if (insdcStatusAttributeOptional.isPresent()) {
          sampleAttributes.removeIf(attribute -> attribute.getType().equals("INSDC Status"));
          sampleAttributes.add(
              Attribute.build(
                  "INSDC Status", "suppressed", "attribute", Collections.emptyList(), null));
        }

        bioSamplesWebinClient.persistSampleResource(
            Sample.Builder.fromSample(sampleInBioSamples).withAttributes(sampleAttributes).build());
      }

      return null;
    }

    final Sample sample;

    if (egaId != null && !egaId.isEmpty()) {
      return egaSampleExporter.populateAndSubmitEgaData(egaId);
    } else {
      try {
        sample = enaSampleToBioSampleConversionService.enrichSample(accession, false);
        boolean success = false;
        int numRetry = 0;

        while (!success) {
          try {
            bioSamplesWebinClient.persistSampleResource(sample);

            success = true;
          } catch (final Exception e) {
            if (++numRetry == MAX_RETRIES) {
              throw new RuntimeException("Failed to handle the sample with accession " + accession);
            }
          }
        }
      } catch (final Exception e) {
        log.info("Failed to handle ENA sample with accession " + accession, e);
      }

      return null;
    }
  }
}
