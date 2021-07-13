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

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.ena.taxonomy.client.TaxonomyClientImpl;
import uk.ac.ebi.ena.taxonomy.taxon.SubmittableTaxon;

@Service
public class ENATaxonClientService extends TaxonomyClientImpl {
  public Sample performTaxonomyValidation(Sample sample) {
    try {
      SubmittableTaxon submittableTaxon =
          getSubmittableTaxonByScientificName(
              sample.getAttributes().stream()
                  .filter(attribute -> attribute.getType().equalsIgnoreCase("Organism"))
                  .findFirst()
                  .get()
                  .getValue());

      if (submittableTaxon.getTaxon() != null) {
        return sample;
      } else {
        throw new ENATaxonUnresolvedException();
      }
    } catch (final Exception e) {
      throw new ENATaxonUnresolvedException();
    }
  }

  @ResponseStatus(
      value = HttpStatus.BAD_REQUEST,
      reason =
          "Validation of taxonomy failed against the ENA taxonomy service. The Organism attribute is either invalid or not submittable")
  public static class ENATaxonUnresolvedException extends RuntimeException {}
}
