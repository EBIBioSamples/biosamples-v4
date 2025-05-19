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
package uk.ac.ebi.biosamples.core.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.core.service.RelationshipValidator;

@RunWith(SpringRunner.class)
public class RelationshipValidationTests {

  private final RelationshipValidator relationshipValidator;

  public RelationshipValidationTests() {
    relationshipValidator = new RelationshipValidator();
  }

  @Test
  public void throws_exception_if_relationship_source_or_target_are_not_accessions() {
    final Relationship rel = Relationship.build("Animal", "derivedFrom", "SAMEG1234123");
    final Collection<String> errors = relationshipValidator.validate(rel);
    assertThat(errors)
        .containsExactly(
            "Source of a relationship must be an accession but was \"" + rel.getSource() + "\"");
  }
}
