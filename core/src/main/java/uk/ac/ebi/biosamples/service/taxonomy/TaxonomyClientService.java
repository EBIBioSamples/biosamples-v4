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
package uk.ac.ebi.biosamples.service.taxonomy;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;
import uk.ac.ebi.ena.taxonomy.client.TaxonomyClientImpl;
import uk.ac.ebi.ena.taxonomy.taxon.SubmittableTaxon;

@Service
public class TaxonomyClientService extends TaxonomyClientImpl {
  public Sample performTaxonomyValidationAndUpdateTaxIdInSample(Sample sample) {
    SubmittableTaxon submittableTaxon;

    try {
      if (sample.getTaxId() != null && sample.getTaxId() != 0) {
        submittableTaxon = getSubmittableTaxonByTaxId(sample.getTaxId());

        if (submittableTaxon.getTaxon() == null) {
          submittableTaxon = getSubmittableTaxonByScientificName(sample);
        }
      } else {
        submittableTaxon = getSubmittableTaxonByScientificName(sample);
      }

      if (submittableTaxon.getTaxon() != null) {
        sample =
            Sample.Builder.fromSample(sample)
                .withTaxId(submittableTaxon.getTaxon().getTaxId())
                .build();
        return sample;
      } else {
        throw new GlobalExceptions.ENATaxonUnresolvedException();
      }
    } catch (final Exception e) {
      throw new GlobalExceptions.ENATaxonUnresolvedException();
    }
  }

  private SubmittableTaxon getSubmittableTaxonByScientificName(final Sample sample) {
    return getSubmittableTaxonByScientificName(
        sample.getAttributes().stream()
            .filter(
                attribute ->
                    (attribute.getType().equalsIgnoreCase("Organism")
                        || attribute.getType().equalsIgnoreCase("Species")))
            .findFirst()
            .get()
            .getValue());
  }
}
