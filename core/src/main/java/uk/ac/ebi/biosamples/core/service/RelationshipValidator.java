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

import java.util.ArrayList;
import java.util.Collection;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.core.model.AccessionType;
import uk.ac.ebi.biosamples.core.model.Relationship;

@Service
public class RelationshipValidator {

  public Collection<String> validate(final Relationship rel) {
    return validate(rel, new ArrayList<>());
  }

  public Collection<String> validate(final Relationship rel, final String accession) {
    // TODO validate that relationships have this sample as the source
    final Collection<String> errors = validate(rel, new ArrayList<>());
    return validateSourceAccession(accession, rel, errors);
  }

  public Collection<String> validate(final Relationship rel, final Collection<String> errors) {
    if (rel.getSource() == null || rel.getSource().isEmpty()) {
      //            errors.add("Source of a relationship must be non empty");//todo re-enable
      // after
      // samepletab deprecation
    } else if (!AccessionType.ANY.matches(rel.getSource())) {
      errors.add(
          "Source of a relationship must be an accession but was \"" + rel.getSource() + "\"");
    }

    if (rel.getTarget() == null || rel.getTarget().isEmpty()) {
      //            errors.add("Target of a relationship must be non empty");//todo re-enable
      // after
      // samepletab deprecation
    } else if (!AccessionType.ANY.matches(rel.getTarget())) {
      errors.add(
          "Target of a relationship must be an accession but was \"" + rel.getTarget() + "\"");
    }

    return errors;
  }

  private Collection<String> validateSourceAccession(
      final String accession, final Relationship rel, final Collection<String> errors) {
    if (accession != null && !accession.equals(rel.getSource())) {
      //            errors.add("Source of the relationship must equal to the sample
      // accession");//
      // todo enable after fixing ENA import pipeline
    }

    return errors;
  }
}
