package uk.ac.ebi.biosamples.service;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.*;
import java.util.stream.Collectors;

public class CuramiUtils {
    private static final double SIMILARITY_THRESHOLD = 0.8;

    public static double getSimilarityScore(String s1, String s2) {
        return new JaroWinklerSimilarity().apply(s1, s2);
    }

    public static List<String> getSimilarAttributes(String attribute, SortedSet<String> attributes) {
        return attributes.stream()
                .filter(a -> CuramiUtils.getSimilarityScore(a, attribute) > 0.8)
                .collect(Collectors.toUnmodifiableList());
    }

    public static Optional<String> getMostSimilarAttribute(String attribute, SortedSet<String> attributes) {
        double similarity = 0;
        Optional<String> similarAttribute = Optional.empty();

        for (String a : attributes) {
            double score = CuramiUtils.getSimilarityScore(a, attribute);
            if (score > SIMILARITY_THRESHOLD && score > similarity) {
                similarity = score;
                similarAttribute = Optional.of(a);
            }
        }

        return similarAttribute;
    }

    public static String normaliseAttribute(String attribute, Set<String> abbreviations) {
        String normalisedAttribute = attribute.replace("_", " ");
        String[] words = normalisedAttribute.split(" ");
        StringJoiner attributeJoiner = new StringJoiner(" ");
        for (String word : words) {
            attributeJoiner.add(abbreviations.contains(word) ? word : word.toLowerCase());
        }
        return attributeJoiner.toString();
    }
}
