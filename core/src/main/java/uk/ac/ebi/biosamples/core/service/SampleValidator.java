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
package uk.ac.ebi.biosamples.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.core.model.Attribute;
import uk.ac.ebi.biosamples.core.model.Relationship;
import uk.ac.ebi.biosamples.core.model.Sample;

@Service
public class SampleValidator {
  private final AttributeValidator attributeValidator;
  private final RelationshipValidator relationshipValidator;

  public SampleValidator(final AttributeValidator attributeValidator) {
    this.attributeValidator = attributeValidator;
    this.relationshipValidator = new RelationshipValidator();
  }

  public Collection<String> validate(final Sample sample) {
    final Collection<String> errors = new ArrayList<>();

    validate(sample, errors);

    return errors;
  }

  public List<String> validate(final Map sampleAsMap) {
    final List<String> errors = new ArrayList<>();

    if (sampleAsMap.get("release") == null) {
      errors.add("Must provide release date in format YYYY-MM-DDTHH:MM:SS");
    }

    if (sampleAsMap.get("name") == null) {
      errors.add("Must provide name");
    }

    final ObjectMapper mapper = new ObjectMapper();

    try {
      final Sample sample = mapper.convertValue(sampleAsMap, Sample.class);
      validate(sample, errors);
    } catch (final IllegalArgumentException e) {
      errors.add(e.getMessage());
    }

    return errors;
  }

  public void validate(final Sample sample, final Collection<String> errors) {
    if (sample.getRelease() == null) {
      errors.add("Must provide release date in format YYYY-MM-DDTHH:MM:SS");
    }

    if (sample.getName() == null) {
      errors.add("Must provide name");
    }

    // TODO more validation
    for (final Attribute attribute : sample.getAttributes()) {
      attributeValidator.validate(attribute, errors);
    }

    for (final Relationship rel : sample.getRelationships()) {
      errors.addAll(relationshipValidator.validate(rel, sample.getAccession()));
    }
  }
}
