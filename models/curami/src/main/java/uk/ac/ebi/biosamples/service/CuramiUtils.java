package uk.ac.ebi.biosamples.service;

import org.apache.commons.text.similarity.JaccardSimilarity;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import java.util.*;
import java.util.stream.Collectors;

public class CuramiUtils {
    public static double getSimilarityScore(String s1, String s2) {
        return new JaroWinklerSimilarity().apply(s1, s2);
//        return new JaccardSimilarity().apply(s1, s2);
    }

    public static List<String> getSimilarAttributes(String attribute, SortedSet<String> attributes) {
        return attributes.stream()
                .filter(a -> CuramiUtils.getSimilarityScore(a, attribute) > 0.8)
                .collect(Collectors.toUnmodifiableList());
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

    public static void main(String[] args) {
        System.out.println(getSimilarityScore("organism", "Organism"));
        System.out.println(CuramiUtils.getSimilarAttributes("organism", new TreeSet<>(Arrays.asList("Organism", "organi", "what", "oops"))));
    }
}
