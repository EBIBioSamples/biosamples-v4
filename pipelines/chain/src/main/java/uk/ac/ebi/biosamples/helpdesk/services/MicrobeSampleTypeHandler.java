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
package uk.ac.ebi.biosamples.helpdesk.services;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.core.model.Attribute;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.core.model.filter.Filter;
import uk.ac.ebi.biosamples.core.service.FilterBuilder;

/**
 * Fetches samples for the MICROBE synthetic community query used in Helpdesk.
 *
 * <p>The underlying public search is:
 *
 * <pre>
 * https://www.ebi.ac.uk/biosamples/samples?filter=attr:project%20name:MICROBE&filter=attr:sample%20type:synthetic%20community
 * </pre>
 */
@Service
public class MicrobeSampleTypeHandler {
  private static final Logger log = LoggerFactory.getLogger(MicrobeSampleTypeHandler.class);
  private static final int DEFAULT_PAGE_SIZE = 20;

  private static final String PROJECT_NAME_ATTRIBUTE = "project name";
  private static final String PROJECT_NAME_VALUE = "MICROBE";
  private static final String SAMPLE_TYPE_ATTRIBUTE = "sample type";
  private static final String SAMPLE_TYPE_VALUE = "synthetic community";

  private final BioSamplesClient bioSamplesClient;

  public MicrobeSampleTypeHandler(@Qualifier("WEBINCLIENT") final BioSamplesClient webinClient) {
    this.bioSamplesClient = webinClient;
  }

  /**
   * Fetches all samples matching the MICROBE synthetic community query into an {@link ArrayList}.
   * The BioSamples cursor endpoint follows HAL {@code next} links until exhausted.
   *
   * @param pageSize page size used on the initial cursor request
   * @return samples collected from all cursor pages
   */
  public ArrayList<Sample> fetchSamples(final int pageSize) {
    final List<Filter> filters =
        List.of(
            FilterBuilder.create()
                .onAttribute(PROJECT_NAME_ATTRIBUTE)
                .withValue(PROJECT_NAME_VALUE)
                .build(),
            FilterBuilder.create()
                .onAttribute(SAMPLE_TYPE_ATTRIBUTE)
                .withValue(SAMPLE_TYPE_VALUE)
                .build());
    final int resolvedPageSize = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
    final ArrayList<Sample> samples = new ArrayList<>();

    for (EntityModel<Sample> model :
        bioSamplesClient.fetchSampleResourceAllWithSize("", filters, resolvedPageSize)) {
      final Sample sample = model.getContent();

      if (sample == null) {
        continue;
      }

      samples.add(sample);

      if (samples.size() % 10000 == 0) {
        log.info(
            "Progress for MICROBE synthetic community fetch: collected {} samples", samples.size());
      }
    }

    log.info(
        "Collected {} MICROBE synthetic community samples using cursor page size {}",
        samples.size(),
        resolvedPageSize);

    return samples;
  }

  public void updateSample(final Sample sample) {
    final String accession = sample.getAccession();
    log.info("Handling sample {} ", accession);

    final List<Attribute> sampleTypeAttributes =
        sample.getAttributes().stream()
            .filter(attr -> attr.getType().equalsIgnoreCase("sample type"))
            .toList();

    if (!sampleTypeAttributes.isEmpty()) {
      if (sampleTypeAttributes.size() > 1) {
        log.info("Multiple sample type attributes for {} ", accession);
      } else {
        final Attribute sampleTypeAttribute = sampleTypeAttributes.get(0);

        sampleTypeAttribute.setValue("defined microbial community");

        final Sample updatedSample = Sample.Builder.fromSample(sample).build();

        bioSamplesClient.persistSampleResource(updatedSample);

        log.info("Handled sample {} ", accession);
      }
    }
  }
}
