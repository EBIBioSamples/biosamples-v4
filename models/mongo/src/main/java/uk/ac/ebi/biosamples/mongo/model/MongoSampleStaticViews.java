package uk.ac.ebi.biosamples.mongo.model;

public enum MongoSampleStaticViews {
    MONGO_SAMPLE_CURATED("mongoSampleCurated");

    private String sampleStaticViewName;

    MongoSampleStaticViews(String sampleStaticViewName) {
        this.sampleStaticViewName = sampleStaticViewName;
    }

    public String getCollectionName() {
        return sampleStaticViewName;
    }
}
