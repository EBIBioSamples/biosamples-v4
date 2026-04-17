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
import uk.ac.ebi.biosamples.core.model.ValidationReport;

@Service
public class SampleValidator {
  private final AttributeValidator attributeValidator;
  private final RelationshipValidator relationshipValidator;

  public SampleValidator(final AttributeValidator attributeValidator) {
    this.attributeValidator = attributeValidator;
    this.relationshipValidator = new RelationshipValidator();
  }

  public Collection<String> validate(final Sample sample) {
    final ValidationReport report = validateStructured(sample);
    final List<String> errors = new ArrayList<>();
    errors.addAll(report.getMissingFields());
    errors.addAll(report.getInvalidTypes());
    errors.addAll(report.getUnknownFields());
    return errors;
  }

  public ValidationReport validateStructured(final Sample sample) {
    final ValidationReport report = new ValidationReport();

    validateStructured(sample, report);

    return report;
  }

  public List<String> validate(final Map sampleAsMap) {
    final ValidationReport report = validateStructured(sampleAsMap);
    final List<String> errors = new ArrayList<>();
    errors.addAll(report.getMissingFields());
    errors.addAll(report.getInvalidTypes());
    errors.addAll(report.getUnknownFields());
    return errors;
  }

  public ValidationReport validateStructured(final Map sampleAsMap) {
    final ValidationReport report = new ValidationReport();

    if (sampleAsMap.get("release") == null) {
      report.addMissingField("release");
    }

    if (sampleAsMap.get("name") == null) {
      report.addMissingField("name");
    }

    final ObjectMapper mapper = new ObjectMapper();

    try {
      final Sample sample = mapper.convertValue(sampleAsMap, Sample.class);
      validateStructured(sample, report);
    } catch (final IllegalArgumentException e) {
      report.addInvalidType(e.getMessage());
    }

    return report;
  }

  public void validateStructured(final Sample sample, final ValidationReport report) {
    if (sample.getRelease() == null) {
      report.addMissingField("release");
    }

    if (sample.getName() == null) {
      report.addMissingField("name");
    }

    boolean hasOrganism = false;
    for (final Attribute attribute : sample.getAttributes()) {
      if ("organism".equalsIgnoreCase(attribute.getType())) {
        hasOrganism = true;
      }
      final List<String> attrErrors = new ArrayList<>();
      attributeValidator.validate(attribute, attrErrors);
      for (final String err : attrErrors) {
        report.addInvalidType(err);
      }
    }

    if (!hasOrganism) {
      report.addMissingField("organism");
    }

    for (final Relationship rel : sample.getRelationships()) {
      final Collection<String> relErrors = relationshipValidator.validate(rel, sample.getAccession());
      for (final String err : relErrors) {
        report.addInvalidType(err);
      }
    }
  }

  @Deprecated
  public void validate(final Sample sample, final Collection<String> errors) {
    final ValidationReport report = validateStructured(sample);
    errors.addAll(report.getMissingFields());
    errors.addAll(report.getInvalidTypes());
    errors.addAll(report.getUnknownFields());
  }
}
