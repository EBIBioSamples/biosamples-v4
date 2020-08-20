package uk.ac.ebi.biosamples.model;

import java.util.List;
import java.util.Objects;

public class AttributeRecommendation implements Comparable<AttributeRecommendation> {
    private final String attribute;
    private final List<String> recommendations;

    private AttributeRecommendation(String attribute, List<String> recommendations) {
        this.attribute = attribute;
        this.recommendations = recommendations;
    }

    public String getAttribute() {
        return attribute;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    @Override
    public String toString() {
        return attribute + "->" + recommendations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof AttributeRecommendation) {
            AttributeRecommendation other = (AttributeRecommendation) o;
            return Objects.equals(this.getAttribute(), other.getAttribute());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute);
    }

    @Override
    public int compareTo(AttributeRecommendation other) {
        return this.attribute.compareTo(other.attribute);
    }

    public static class Builder {
        private String attribute;
        private List<String> recommendations;

        public Builder withAttribute(String attribute) {
            this.attribute = attribute;
            return this;
        }

        public Builder withRecommendations(List<String> recommendations) {
            this.recommendations = recommendations;
            return this;
        }

        public Builder withRecommendation(String recommendation) {
            recommendations.add(recommendation);
            return this;
        }

        public AttributeRecommendation build() {
            return new AttributeRecommendation(attribute, recommendations);
        }
    }
}
