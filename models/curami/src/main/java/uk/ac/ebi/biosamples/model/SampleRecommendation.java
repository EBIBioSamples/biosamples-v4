package uk.ac.ebi.biosamples.model;

public class SampleRecommendation {
    private CuramiRecommendation recommendations;
    private Sample sample;

    public SampleRecommendation(CuramiRecommendation recommendations, Sample sample) {
        this.recommendations = recommendations;
        this.sample = sample;
    }

    public CuramiRecommendation getRecommendations() {
        return recommendations;
    }

    public Sample getSample() {
        return sample;
    }
}
