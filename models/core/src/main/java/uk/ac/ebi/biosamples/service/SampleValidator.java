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
package uk.ac.ebi.biosamples.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Relationship;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class SampleValidator {
  private static final String VALIDATION_MESSAGE =
      "Only Sample name, sample accession and sample structured data can be provided through this API";
  private static final String NO_STRUCTURED_DATA_IS_PROVIDED = "No structured data is provided";
  private final AttributeValidator attributeValidator;
  private final RelationshipValidator relationshipValidator;

  public SampleValidator(AttributeValidator attributeValidator) {
    this.attributeValidator = attributeValidator;
    this.relationshipValidator = new RelationshipValidator();
  }

  public Collection<String> validate(Sample sample) {
    Collection<String> errors = new ArrayList<String>();
    validate(sample, errors);
    return errors;
  }

  public void validateSampleContentsForStructuredDataPatching(Sample newSample) {
    assert newSample.getData() != null;

    final String domain = newSample.getDomain();

    if (!(newSample.getData().size() > 0)) {
      throw new RuntimeException(NO_STRUCTURED_DATA_IS_PROVIDED);
    }

    if (newSample.getAttributes() != null && newSample.getAttributes().size() > 0) {
      throw new RuntimeException(VALIDATION_MESSAGE);
    }

    if (newSample.getExternalReferences() != null && newSample.getExternalReferences().size() > 0) {
      throw new RuntimeException(VALIDATION_MESSAGE);
    }

    if (newSample.getRelationships() != null && newSample.getRelationships().size() > 0) {
      throw new RuntimeException(VALIDATION_MESSAGE);
    }

    if (newSample.getContacts() != null && newSample.getContacts().size() > 0) {
      throw new RuntimeException(VALIDATION_MESSAGE);
    }

    if (newSample.getPublications() != null && newSample.getPublications().size() > 0) {
      throw new RuntimeException(VALIDATION_MESSAGE);
    }

    if (domain != null && domain.length() > 0) {
      throw new RuntimeException(VALIDATION_MESSAGE);
    }

    if (!newSample.hasAccession()) {
      throw new RuntimeException("Sample doesn't have an accession");
    }
  }

  public List<String> validate(Map sampleAsMap) {
    List<String> errors = new ArrayList<>();
    if (sampleAsMap.get("release") == null) {
      errors.add("Must provide release date in format YYYY-MM-DDTHH:MM:SS");
    }

    if (sampleAsMap.get("name") == null) {
      errors.add("Must provide name");
    }

    ObjectMapper mapper = new ObjectMapper();
    try {
      Sample sample = mapper.convertValue(sampleAsMap, Sample.class);
      validate(sample, errors);
    } catch (IllegalArgumentException e) {
      errors.add(e.getMessage());
    }

    return errors;
  }

  public void validate(Sample sample, Collection<String> errors) {

    if (sample.getRelease() == null) {
      errors.add("Must provide release date in format YYYY-MM-DDTHH:MM:SS");
    }

    if (sample.getName() == null) {
      errors.add("Must provide name");
    }

    // TODO more validation
    for (Attribute attribute : sample.getAttributes()) {
      attributeValidator.validate(attribute, errors);
    }

    for (Relationship rel : sample.getRelationships()) {
      errors.addAll(relationshipValidator.validate(rel, sample.getAccession()));
    }
  }
}
