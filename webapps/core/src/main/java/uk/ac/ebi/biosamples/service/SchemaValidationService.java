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
package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.exception.SampleValidationException;
import uk.ac.ebi.biosamples.exception.SchemaValidationException;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.validation.ValidatorI;

@Service
public class SchemaValidationService {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final BioSamplesProperties bioSamplesProperties;
  private final ObjectMapper objectMapper;
  private final ValidatorI validator;

  public SchemaValidationService(
      ObjectMapper mapper, BioSamplesProperties bioSamplesProperties, @Qualifier("elixirValidator") ValidatorI validator) {
    this.objectMapper = mapper;
    this.bioSamplesProperties = bioSamplesProperties;
    this.validator = validator;
  }

  public String validate(Sample sample) {
    String schemaId =
        sample.getCharacteristics().stream()
            .filter(
                s ->
                    s.getType()
                        .equalsIgnoreCase(
                            "checklist"))
            // to search
            .findFirst()
            .map(Attribute::getValue)
            .orElse(bioSamplesProperties.getBiosamplesDefaultSchema());

    try {
      String sampleString = this.objectMapper.writeValueAsString(sample);
      return validator.validateById(schemaId, sampleString);
    } catch (ValidationException | SampleValidationException e) {
      throw new SchemaValidationException("Checklist validation failed: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("Schema validation error: " + e.getMessage(), e);
      throw new SchemaValidationException("Checklist validation error: " + e.getMessage(), e);
    }
  }
}
