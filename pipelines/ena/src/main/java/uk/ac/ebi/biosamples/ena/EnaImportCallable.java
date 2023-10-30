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
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.ega.EgaSampleExporter;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SampleStatus;
import uk.ac.ebi.biosamples.service.EnaSampleToBioSampleConversionService;
import uk.ac.ebi.biosamples.service.EraProDao;
import uk.ac.ebi.biosamples.service.EraproSample;

public class EnaImportCallable implements Callable<Void> {
  private static final String SRA_ACCESSION = "SRA accession";
  private static final int MAX_RETRIES = 5;
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final BioSamplesClient bioSamplesWebinClient;
  private final BioSamplesClient bioSamplesAapClient;
  private final EgaSampleExporter egaSampleExporter;
  private final EnaSampleToBioSampleConversionService enaSampleToBioSampleConversionService;
  private final EraProDao eraProDao;
  private final SpecialTypes specialTypes;
  private final String accession;
  private final String egaId;

  EnaImportCallable(
      final String accession,
      final String egaId,
      final BioSamplesClient bioSamplesWebinClient,
      final BioSamplesClient bioSamplesAapClient,
      final EgaSampleExporter egaSampleExporter,
      final EnaSampleToBioSampleConversionService enaSampleToBioSampleConversionService,
      final EraProDao eraProDao,
      final SpecialTypes specialTypes) {
    this.accession = accession;
    this.egaId = egaId;
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.bioSamplesAapClient = bioSamplesAapClient;
    this.egaSampleExporter = egaSampleExporter;
    this.enaSampleToBioSampleConversionService = enaSampleToBioSampleConversionService;
    this.eraProDao = eraProDao;
    this.specialTypes = specialTypes;
  }

  @Override
  public Void call() throws Exception {
    if (specialTypes != null
        && (specialTypes.equals(SpecialTypes.SUPPRESSED)
            || specialTypes.equals(SpecialTypes.KILLED))) {
      return handleSuppressedKilledSample(specialTypes);
    }

    Sample enaSampleConvertedToBioSample = null;

    if (egaId != null && !egaId.isEmpty()) {
      return egaSampleExporter.populateAndSubmitEgaData(egaId);
    } else {
      try {
        Sample bsdAuthoritySampleWithSraAccession = null;

        if (specialTypes == SpecialTypes.BSD_AUTHORITY) {
          bsdAuthoritySampleWithSraAccession = buildBsdAuthoritySampleWithSraAccession(accession);
        } else {
          enaSampleConvertedToBioSample =
              enaSampleToBioSampleConversionService.enrichSample(accession, false);
        }

        boolean success = false;
        int numRetry = 0;

        while (!success) {
          try {
            if (specialTypes == SpecialTypes.BSD_AUTHORITY) {
              if (bsdAuthoritySampleWithSraAccession != null) {
                if (bsdAuthoritySampleWithSraAccession.getDomain() != null) {
                  bioSamplesAapClient.persistSampleResource(bsdAuthoritySampleWithSraAccession);
                } else if (bsdAuthoritySampleWithSraAccession.getWebinSubmissionAccountId()
                    != null) {
                  bioSamplesWebinClient.persistSampleResource(bsdAuthoritySampleWithSraAccession);
                } else {
                  throw new RuntimeException(
                      "Couldn't determine authentication of sample: " + accession);
                }
              } else {
                throw new RuntimeException(
                    "Failed to fetch BioSample authority sample from BioSamples: " + accession);
              }
            } else {
              if (enaSampleConvertedToBioSample != null) {
                bioSamplesWebinClient.persistSampleResource(enaSampleConvertedToBioSample);
              } else {
                throw new RuntimeException(
                    "ENA sample converted to BioSample is null: " + accession);
              }
            }

            success = true;
          } catch (final Exception e) {
            if (++numRetry == MAX_RETRIES) {
              EnaImportRunner.failures.add(accession);

              throw new RuntimeException(
                  "Failed to handle the sample with accession: " + accession);
            }
          }
        }
      } catch (final Exception e) {
        log.info("Failed to handle ENA sample with accession: " + accession, e);

        throw e;
      }

      return null;
    }
  }

  private Sample buildBsdAuthoritySampleWithSraAccession(final String accession) {
    final Optional<EntityModel<Sample>> sampleOptionalInBioSamples =
        bioSamplesWebinClient.fetchSampleResource(
            accession, Optional.of(Collections.singletonList("")));
    final Sample sampleInBioSamples =
        sampleOptionalInBioSamples.map(EntityModel::getContent).orElse(null);
    final EraproSample eraproSample = eraProDao.getSampleDetailsByBioSampleId(accession);

    assert sampleInBioSamples != null;

    final Set<Attribute> attributesInBioSample = sampleInBioSamples.getAttributes();

    if (attributesInBioSample.stream()
        .noneMatch(attribute -> attribute.getType().equals(SRA_ACCESSION))) {
      attributesInBioSample.add(Attribute.build(SRA_ACCESSION, eraproSample.getSampleId()));
    }

    return Sample.Builder.fromSample(sampleInBioSamples)
        .withAttributes(attributesInBioSample)
        .build();
  }

  private Void handleSuppressedKilledSample(final SpecialTypes specialTypes)
      throws DocumentException {
    final Optional<EntityModel<Sample>> sampleOptionalInBioSamples =
        bioSamplesWebinClient.fetchSampleResource(
            accession, Optional.of(Collections.singletonList("")));
    final Sample sampleInBioSamples =
        sampleOptionalInBioSamples.map(EntityModel::getContent).orElse(null);
    final String statusHandled = specialTypes.name().toLowerCase();

    if (sampleInBioSamples != null) {
      final Set<Attribute> sampleAttributes = sampleInBioSamples.getAttributes();
      final Attribute insdcStatusAttribute =
          sampleAttributes.stream()
              .filter(attribute -> attribute.getType().equals("INSDC Status"))
              .findFirst()
              .orElse(null);

      if (insdcStatusAttribute == null) {
        log.info(
            "Sample exists in BioSamples and INSDC status is not set, adding INSDC status as "
                + statusHandled
                + " for "
                + accession);

        sampleAttributes.add(Attribute.build("INSDC Status", statusHandled));

        bioSamplesWebinClient.persistSampleResource(
            Sample.Builder.fromSample(sampleInBioSamples)
                .withAttributes(sampleAttributes)
                .withStatus(SampleStatus.valueOf(String.valueOf(specialTypes)))
                .build());

        addToList(specialTypes);
      } else if (!insdcStatusAttribute.getValue().equalsIgnoreCase(statusHandled)) {
        log.info(
            "Sample exists in BioSamples and INSDC status is not "
                + statusHandled
                + ", adding INSDC status as "
                + statusHandled
                + " for "
                + accession);

        sampleAttributes.remove(insdcStatusAttribute);
        sampleAttributes.add(Attribute.build("INSDC Status", statusHandled));

        bioSamplesWebinClient.persistSampleResource(
            Sample.Builder.fromSample(sampleInBioSamples)
                .withAttributes(sampleAttributes)
                .withStatus(SampleStatus.valueOf(String.valueOf(specialTypes)))
                .build());

        addToList(specialTypes);
      } else {
        log.info(
            "Sample exists in BioSamples and INSDC status is "
                + statusHandled
                + " ,no change required for "
                + accession);

        bioSamplesWebinClient.persistSampleResource(
            Sample.Builder.fromSample(sampleInBioSamples)
                .withStatus(SampleStatus.valueOf(String.valueOf(specialTypes)))
                .build());

        addToList(specialTypes);
      }
    } else {
      log.info(
          "Sample doesn't exist in BioSamples, fetching "
              + sampleInBioSamples
              + " sample from ERAPRO "
              + accession);
      try {
        final Sample sample = enaSampleToBioSampleConversionService.enrichSample(accession, false);

        boolean success = false;
        int numRetry = 0;

        while (!success) {
          try {
            bioSamplesWebinClient.persistSampleResource(sample);

            addToList(specialTypes);

            success = true;
          } catch (final Exception e) {
            if (++numRetry == MAX_RETRIES) {
              EnaImportRunner.failures.add(accession);

              throw new RuntimeException(
                  "Failed to handle the ENA suppressed/ killed sample with accession " + accession);
            }
          }
        }
      } catch (final Exception e) {
        log.info("Failed to handle ENA suppressed/ killed sample with accession " + accession, e);

        throw e;
      }
    }

    return null;
  }

  private void addToList(final SpecialTypes specialTypes) {
    if (specialTypes.equals(SpecialTypes.SUPPRESSED)) {
      EnaImportRunner.todaysSuppressedSamples.add(accession);
    } else {
      EnaImportRunner.todaysKilledSamples.add(accession);
    }
  }
}
