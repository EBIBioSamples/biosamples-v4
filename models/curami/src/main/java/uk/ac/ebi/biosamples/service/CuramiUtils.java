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

import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

public class CuramiUtils {
  private static final double SIMILARITY_THRESHOLD = 0.8;

  private static double getSimilarityScore(final String s1, final String s2) {
    return new JaroWinklerSimilarity().apply(s1, s2);
  }

  public static List<String> getSimilarAttributes(
      final String attribute, final SortedSet<String> attributes) {
    return attributes.stream()
        .filter(a -> CuramiUtils.getSimilarityScore(a, attribute) > 0.8)
        .collect(Collectors.toList());
  }

  static Optional<String> getMostSimilarAttribute(
      final String attribute, final SortedSet<String> attributes) {
    double similarity = 0;
    Optional<String> similarAttribute = Optional.empty();

    for (final String a : attributes) {
      final double score = CuramiUtils.getSimilarityScore(a, attribute);
      if (score > SIMILARITY_THRESHOLD && score > similarity) {
        similarity = score;
        similarAttribute = Optional.of(a);
      }
    }

    return similarAttribute;
  }

  static String normaliseAttribute(final String attribute, final Set<String> abbreviations) {
    final String normalisedAttribute = attribute.replace("_", " ");
    final String[] words = normalisedAttribute.split(" ");
    final StringJoiner attributeJoiner = new StringJoiner(" ");
    for (final String word : words) {
      attributeJoiner.add(abbreviations.contains(word) ? word : word.toLowerCase());
    }
    return attributeJoiner.toString();
  }
}
