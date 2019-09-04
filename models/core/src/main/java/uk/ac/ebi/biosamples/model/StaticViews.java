package uk.ac.ebi.biosamples.model;

import java.util.Collection;

public class StaticViews {
    public static final String SAMPLE_CURATED_REPO = "curated";
    public static final String SAMPLE_PRIMARY_REPO = "none";

    public static final String SAMPLE_CURATED_REPO_MONGO = "mongoSampleCurated";
    public static final String SAMPLE_PRIMARY_REPO_MONGO = "mongoSample";

    public static MongoSampleStaticViews getStaticView(Collection<String> domains, String curationRepo) {
        MongoSampleStaticViews staticView;
        //todo change default actions once static view is stable
        if (domains != null) {
            staticView = MongoSampleStaticViews.MONGO_SAMPLE_DYNAMIC;
        } else if (curationRepo == null || curationRepo.isEmpty()) {
            staticView = MongoSampleStaticViews.MONGO_SAMPLE_CURATED;
        } else if (SAMPLE_PRIMARY_REPO.equalsIgnoreCase(curationRepo)) {
            staticView = MongoSampleStaticViews.MONGO_SAMPLE_DYNAMIC;
        } else if (SAMPLE_CURATED_REPO.equalsIgnoreCase(curationRepo)) {
            staticView = MongoSampleStaticViews.MONGO_SAMPLE_CURATED;
        } else {
            staticView = MongoSampleStaticViews.MONGO_SAMPLE_CURATED;
        }

        return staticView;
    }

    public enum MongoSampleStaticViews {
        MONGO_SAMPLE_CURATED(StaticViews.SAMPLE_CURATED_REPO, StaticViews.SAMPLE_CURATED_REPO_MONGO),
        MONGO_SAMPLE_DYNAMIC(StaticViews.SAMPLE_PRIMARY_REPO, StaticViews.SAMPLE_PRIMARY_REPO_MONGO);

        private String sampleStaticViewName;
        private String curationRepo;

        MongoSampleStaticViews(String curationRepo, String sampleStaticViewName) {
            this.sampleStaticViewName = sampleStaticViewName;
            this.curationRepo = curationRepo;
        }

        public String getCollectionName() {
            return sampleStaticViewName;
        }

        public String getCurationRepositoryName() {
            return curationRepo;
        }
    }
}
