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
package uk.ac.ebi.biosamples.service.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.ac.ebi.biosamples.BioSamplesConstants;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.core.model.Attribute;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.exception.GlobalExceptions;

@Service
public class SchemaValidationService {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final BioSamplesProperties bioSamplesProperties;
  private final ObjectMapper objectMapper;
  private final ValidatorI validator;

  public SchemaValidationService(
      final ObjectMapper mapper,
      final BioSamplesProperties bioSamplesProperties,
      @Qualifier("elixirValidator") final ValidatorI validator) {
    objectMapper = mapper;
    this.bioSamplesProperties = bioSamplesProperties;
    this.validator = validator;
  }

  public Sample validate(final Sample sample, final String webinId) {
    final String schemaId =
        sample.getCharacteristics().stream()
            .filter(
                s -> BioSamplesConstants.CHECKLIST_ATTRIBUTES.contains(s.getType().toLowerCase()))
            .findFirst()
            .map(Attribute::getValue)
            .orElse(bioSamplesProperties.getBiosamplesDefaultSchema());

    // check if the schema referred in the sample is accessible or protected
    if (schemaId.equalsIgnoreCase(bioSamplesProperties.getBiosamplesRestrictedSchema())
        && !bioSamplesProperties.getBiosamplesClientWebinUsername().equals(webinId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Validation of sample metadata against this schema is not permitted");
    }

    try {
      final String sampleString = objectMapper.writeValueAsString(sample);

      validator.validateById(schemaId, sampleString);

      /*if (sampleAttributes.stream()
          .noneMatch(attribute -> attribute.getType().equalsIgnoreCase("checklist"))) {
        sampleAttributes.add(Attribute.build("checklist", schemaId));

        return Sample.Builder.fromSample(sample).withAttributes(sampleAttributes).build();
      }*/

      return sample;
    } catch (final ValidationException | GlobalExceptions.SampleValidationException e) {
      throw new GlobalExceptions.SchemaValidationException(e.getMessage(), e);
    } catch (final Exception e) {
      log.error("Schema validation error: " + e.getMessage(), e);

      throw new GlobalExceptions.SchemaValidationException(
          "Sample validation error: " + e.getMessage(), e);
    }
  }
}
