/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.legacy.json.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Optional;
import uk.ac.ebi.biosamples.legacy.json.domain.SamplesRelations;
import uk.ac.ebi.biosamples.legacy.json.repository.SampleRepository;
import uk.ac.ebi.biosamples.model.Sample;

public class SampleRelationsDeserializer extends JsonDeserializer<SamplesRelations> {

  private final SampleRepository sampleRepository;

  public SampleRelationsDeserializer(SampleRepository sampleRepository) {
    this.sampleRepository = sampleRepository;
  }

  @Override
  public SamplesRelations deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    String accession = node.get("accession").textValue();
    Optional<Sample> sample = sampleRepository.findByAccession(accession);
    return sample.map(SamplesRelations::new).orElse(null);
  }
}
