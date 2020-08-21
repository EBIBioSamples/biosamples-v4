package uk.ac.ebi.biosamples.model;

import java.util.Objects;

public class AttributeRecommendation implements Comparable<AttributeRecommendation> {
    private final String attribute;
    private final String recommendation;

    private AttributeRecommendation(String attribute, String recommendations) {
        this.attribute = attribute;
        this.recommendation = recommendations;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getRecommendation() {
        return recommendation;
    }

    @Override
    public String toString() {
        return attribute + "->" + recommendation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof AttributeRecommendation) {
            AttributeRecommendation other = (AttributeRecommendation) o;
            return Objects.equals(this.getAttribute(), other.getAttribute())
                    && Objects.equals(this.getRecommendation(), other.getRecommendation());
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
        private String recommendation;

        public Builder withAttribute(String attribute) {
            this.attribute = attribute;
            return this;
        }

        public Builder withRecommendation(String recommendations) {
            this.recommendation = recommendations;
            return this;
        }

        public AttributeRecommendation build() {
            return new AttributeRecommendation(attribute, recommendation);
        }
    }
}
