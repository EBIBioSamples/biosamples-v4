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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.biosamples.model.Sample;

public class TaxonomyClientServiceTest {
  @Test
  public void validateSample() throws IOException {
    File file =
        new File(
            getClass()
                .getClassLoader()
                .getResource("json/ncbi-SAMN03894263-curated-no-data.json")
                .getFile());
    ObjectMapper objectMapper = new ObjectMapper();
    TaxonomyClientService taxonomyClientService = new TaxonomyClientService();
    Sample sample = objectMapper.readValue(file, Sample.class);

    sample = taxonomyClientService.performTaxonomyValidationAndUpdateTaxIdInSample(sample, true);

    final String organismInSample =
        sample.getAttributes().stream()
            .filter(attr -> attr.getType().equalsIgnoreCase("Organism"))
            .findFirst()
            .get()
            .getValue();

    Assert.assertTrue(organismInSample != null);
  }
}
