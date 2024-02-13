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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.ExternalReference;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.SubmittedViaType;
import uk.ac.ebi.biosamples.model.structured.StructuredData;
import uk.ac.ebi.biosamples.model.structured.StructuredDataTable;
import uk.ac.ebi.biosamples.ncbi.service.NcbiSampleConversionService;

public class NcbiElementCallable implements Callable<Void> {
  private static final int MAX_RETRIES = 5;

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final Element sampleElem;
  private final String domain;
  private final BioSamplesClient bioSamplesClient;
  private final NcbiSampleConversionService ncbiSampleConversionService;
  private final Map<String, Set<StructuredDataTable>> sampleToAmrMap;

  NcbiElementCallable(
      final NcbiSampleConversionService ncbiSampleConversionService,
      final BioSamplesClient bioSamplesClient,
      final Element sampleElem,
      final String domain,
      final Map<String, Set<StructuredDataTable>> sampleToAmrMap) {
    this.ncbiSampleConversionService = ncbiSampleConversionService;
    this.bioSamplesClient = bioSamplesClient;
    this.sampleElem = sampleElem;
    this.domain = domain;
    this.sampleToAmrMap = sampleToAmrMap;
  }

  @Override
  public Void call() {
    final String accession = sampleElem.attributeValue("accession");

    try {
      boolean success = false;
      int numRetry = 0;

      Set<StructuredDataTable> amrData = new HashSet<>();

      log.trace("Element callable starting for " + accession);

      if (sampleToAmrMap != null && sampleToAmrMap.containsKey(accession)) {
        amrData = sampleToAmrMap.get(accession);
      }

      // Generate the sample without the domain
      final Sample sampleWithoutDomain =
          ncbiSampleConversionService.convertNcbiXmlElementToSample(sampleElem);
      final Sample sample =
          Sample.Builder.fromSample(sampleWithoutDomain)
              .withDomain(domain)
              .withSubmittedVia(SubmittedViaType.PIPELINE_IMPORT)
              .build();
      final ExternalReference exRef =
          ExternalReference.build("https://www.ebi.ac.uk/ena/browser/view/" + accession);
      final Curation curation = Curation.build(null, null, null, Collections.singleton(exRef));

      while (!success) {
        try {
          bioSamplesClient.persistSampleResource(sample);

          success = true;

          try {
            bioSamplesClient.persistCuration(accession, curation, domain, false);
          } catch (final Exception e) {
            log.info("Failed to curate NCBI sample with ENA link " + accession);
          }
        } catch (final Exception e) {
          if (++numRetry == MAX_RETRIES) {
            throw new RuntimeException("Failed to handle the sample with accession " + accession);
          }
        }
      }

      final Set<StructuredDataTable> structuredDataTableSet =
          ncbiSampleConversionService.convertNcbiXmlElementToStructuredData(sampleElem, amrData);

      if (!structuredDataTableSet.isEmpty()) {
        final StructuredData structuredData =
            StructuredData.build(accession, sample.getCreate(), structuredDataTableSet);
        /*bioSamplesClient.persistStructuredData(structuredData);*/
      }

      log.trace("Element callable finished");
    } catch (final Exception e) {
      log.info("Failed to import NCBI sample having accession " + accession);
    }

    return null;
  }

  /** Safe way to extract the taxonomy id from the string */
  private int getTaxId(final String value) {
    if (value == null) {
      throw new RuntimeException("Unable to extract tax id from a null value");
    }
    return Integer.parseInt(value.trim());
  }
}
