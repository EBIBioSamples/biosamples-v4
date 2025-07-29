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

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import uk.ac.ebi.biosamples.core.model.Relationship;
import uk.ac.ebi.biosamples.core.model.Sample;

public class SampleRelationshipUtils {

  /**
   * Given a sample, get the collection of relationships where this sample is the source.
   *
   * <p>Sample will be the source if it matches the accession, or if it is blank if the sample has
   * no accession.
   */
  public static SortedSet<Relationship> getOutgoingRelationships(final Sample sample) {
    final SortedSet<Relationship> relationships = new TreeSet<>();
    for (final Relationship relationship : sample.getRelationships()) {
      if (!sample.hasAccession()
          && (relationship.getSource() == null || relationship.getSource().trim().length() == 0)) {
        relationships.add(relationship);
      } else if (relationship.getSource() != null
          && relationship.getSource().equals(sample.getAccession())) {
        relationships.add(relationship);
      }
    }

    return relationships;
  }

  /** Given a sample, get the collection of relationships where this sample is the target. */
  public static SortedSet<Relationship> getIncomingRelationships(final Sample sample) {
    return sample.getRelationships().stream()
        .filter(rel -> rel.getTarget().equals(sample.getAccession()))
        .collect(Collectors.toCollection(TreeSet::new));
  }
}
