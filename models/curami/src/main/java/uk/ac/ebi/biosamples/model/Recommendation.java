package uk.ac.ebi.biosamples.model;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

public class Recommendation {
    private int quality;
    private SortedSet<String> goodAttributes;
    private SortedSet<AttributeRecommendation> badAttributes;
    private SortedSet<String> missingAttributes;

    private Recommendation(int quality, SortedSet<String> goodAttributes, SortedSet<AttributeRecommendation> badAttributes, SortedSet<String> missingAttributes) {
        this.quality = quality;
        this.goodAttributes = goodAttributes;
        this.badAttributes = badAttributes;
        this.missingAttributes = missingAttributes;
    }

    public int getQuality() {
        return quality;
    }

    public SortedSet<String> getGoodAttributes() {
        return goodAttributes;
    }

    public SortedSet<AttributeRecommendation> getBadAttributes() {
        return badAttributes;
    }

    public SortedSet<String> getMissingAttributes() {
        return missingAttributes;
    }

    public static class Builder {
        private int quality;
        private SortedSet<String> goodAttributes;
        private SortedSet<AttributeRecommendation> badAttributes;
        private SortedSet<String> missingAttributes;

        public Builder() {
            this.goodAttributes = new TreeSet<>();
            this.badAttributes = new TreeSet<>();
            this.missingAttributes = new TreeSet<>();
        }

        public Builder withQuality(int quality) {
            this.quality = Math.min(quality, 100);
            this.quality = Math.max(quality, 0);
            return this;
        }

        public Builder withGoodAttributes(SortedSet<String> goodAttributes) {
            this.goodAttributes = goodAttributes;
            return this;
        }

        public Builder withBadAttributes(SortedSet<AttributeRecommendation> badAttributes) {
            this.badAttributes = badAttributes;
            return this;
        }

        public Builder withMissingAttributes(SortedSet<String> missingAttributes) {
            this.missingAttributes = missingAttributes;
            return this;
        }

        public Recommendation build() {
            return new Recommendation(quality, goodAttributes, badAttributes, missingAttributes);
        }
    }
}
