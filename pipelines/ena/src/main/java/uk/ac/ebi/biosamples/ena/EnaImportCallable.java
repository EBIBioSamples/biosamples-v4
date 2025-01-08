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

import static uk.ac.ebi.biosamples.BioSamplesConstants.SRA_ACCESSION;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import uk.ac.ebi.biosamples.BioSamplesConstants;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SampleStatus;
import uk.ac.ebi.biosamples.service.EnaSampleToBioSampleConversionService;
import uk.ac.ebi.biosamples.service.EraProDao;
import uk.ac.ebi.biosamples.service.EraproSample;

public class EnaImportCallable implements Callable<Void> {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final BioSamplesClient bioSamplesWebinClient;
  private final BioSamplesClient bioSamplesAapClient;
  private final EnaSampleToBioSampleConversionService enaSampleToBioSampleConversionService;
  private final EraProDao eraProDao;
  private final SpecialTypes specialTypes;
  private final String accession;

  EnaImportCallable(
      final String accession,
      final BioSamplesClient bioSamplesWebinClient,
      final BioSamplesClient bioSamplesAapClient,
      final EnaSampleToBioSampleConversionService enaSampleToBioSampleConversionService,
      final EraProDao eraProDao,
      final SpecialTypes specialTypes) {
    this.accession = accession;
    this.bioSamplesWebinClient = bioSamplesWebinClient;
    this.bioSamplesAapClient = bioSamplesAapClient;
    this.enaSampleToBioSampleConversionService = enaSampleToBioSampleConversionService;
    this.eraProDao = eraProDao;
    this.specialTypes = specialTypes;
  }

  @Override
  public Void call() throws Exception {
    log.info("Handling " + accession);

    if (specialTypes != null
        && (specialTypes.equals(SpecialTypes.SUPPRESSED)
            || specialTypes.equals(SpecialTypes.KILLED))) {
      return handleSuppressedKilledSample(specialTypes);
    }

    Sample enaSampleConvertedToBioSample = null;

    try {
      SampleToUpdateRequiredPair sampleToUpdateRequiredPair = null;

      if (specialTypes == SpecialTypes.BSD_AUTHORITY) {
        sampleToUpdateRequiredPair = buildBsdAuthoritySampleWithSraAccession(accession);
      } else {
        enaSampleConvertedToBioSample =
            enaSampleToBioSampleConversionService.enrichSample(accession);
      }

      boolean success = false;
      int numRetry = 0;

      while (!success) {
        try {
          if (specialTypes == SpecialTypes.BSD_AUTHORITY) {
            if (sampleToUpdateRequiredPair.updateRequired) {
              final Sample bsdAuthoritySampleWithSraAccession = sampleToUpdateRequiredPair.sample;

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
            }
          } else {
            if (enaSampleConvertedToBioSample != null) {
              bioSamplesWebinClient.persistSampleResource(enaSampleConvertedToBioSample);
            } else {
              throw new RuntimeException("ENA sample converted to BioSample is null: " + accession);
            }
          }

          success = true;
        } catch (final Exception e) {
          if (++numRetry == BioSamplesConstants.MAX_RETRIES) {
            EnaImportRunner.failures.add(accession);

            throw new RuntimeException("Failed to handle the sample with accession: " + accession);
          }
        }
      }
    } catch (final Exception e) {
      log.info("Failed to handle ENA sample with accession: " + accession, e);

      throw e;
    }

    return null;
  }

  private SampleToUpdateRequiredPair buildBsdAuthoritySampleWithSraAccession(
      final String accession) {
    final Optional<EntityModel<Sample>> sampleOptionalInBioSamples =
        bioSamplesWebinClient.fetchSampleResource(accession, false);
    final Sample sampleInBioSamples =
        sampleOptionalInBioSamples.map(EntityModel::getContent).orElse(null);
    final EraproSample eraproSample = eraProDao.getSampleDetailsByBioSampleId(accession);
    final String eraproSampleSampleId = eraproSample.getSampleId();
    boolean sampleSaveRequired = false;

    assert sampleInBioSamples != null;

    final Set<Attribute> attributesInBioSample = sampleInBioSamples.getAttributes();

    if (attributesInBioSample.stream()
        .noneMatch(attribute -> attribute.getType().equals(SRA_ACCESSION))) {
      log.info(
          "Sample "
              + accession
              + " doesn't have SRA accession, creating new SRA accession attribute with SAMPLE_ID from ENA");

      sampleSaveRequired = true;
      attributesInBioSample.add(Attribute.build(SRA_ACCESSION, eraproSampleSampleId));
    } else {
      final Attribute sraAccessionAttribute =
          attributesInBioSample.stream()
              .filter(attribute -> attribute.getType().equals(SRA_ACCESSION))
              .findFirst()
              .get();

      if (!Objects.equals(sraAccessionAttribute.getValue(), eraproSampleSampleId)) {
        log.info(
            "Sample "
                + accession
                + " has SRA accession mismatch with ENA, this shouldn't happen - investigate");

        /*sampleSaveRequired = true;
        attributesInBioSample.removeIf(attribute -> attribute.getType().equals(SRA_ACCESSION));
        attributesInBioSample.add(Attribute.build(SRA_ACCESSION, eraproSampleSampleId));*/
        // August 12, 2024: dont do anything to these samples, ENA and BSD auth samples shouldn't
        // have this mismatch from the end of 2023
      } else {
        log.info("Sample " + accession + " has SRA accession match with ENA, no action required");
      }
    }

    return new SampleToUpdateRequiredPair(
        Sample.Builder.fromSample(sampleInBioSamples).withAttributes(attributesInBioSample).build(),
        sampleSaveRequired);
  }

  private static class SampleToUpdateRequiredPair {
    private final Sample sample;
    private final boolean updateRequired;

    private SampleToUpdateRequiredPair(final Sample sample, final boolean updateRequired) {
      this.sample = sample;
      this.updateRequired = updateRequired;
    }
  }

  private Void handleSuppressedKilledSample(final SpecialTypes specialTypes)
      throws DocumentException {
    final Optional<EntityModel<Sample>> sampleOptionalInBioSamples =
        bioSamplesWebinClient.fetchSampleResource(accession, false);
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
        final Sample sample = enaSampleToBioSampleConversionService.enrichSample(accession);

        boolean success = false;
        int numRetry = 0;

        while (!success) {
          try {
            bioSamplesWebinClient.persistSampleResource(sample);

            addToList(specialTypes);

            success = true;
          } catch (final Exception e) {
            if (++numRetry == BioSamplesConstants.MAX_RETRIES) {
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
