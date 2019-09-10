package uk.ac.ebi.biosamples.model;

import java.util.Collection;

public class StaticViewWrapper {
    public static final String SAMPLE_CURATED_REPO = "curated";
    public static final String SAMPLE_PRIMARY_REPO = "none";

    public static final String SAMPLE_CURATED_REPO_MONGO = "mongoSampleCurated";
    public static final String SAMPLE_PRIMARY_REPO_MONGO = "mongoSample";

    public static StaticView getStaticView(Collection<String> domains, String curationRepo) {
        StaticView staticView;
        if (domains != null) {
            staticView = StaticView.SAMPLES_DYNAMIC;
        } else if (curationRepo == null || curationRepo.isEmpty()) {
            staticView = StaticView.SAMPLES_CURATED;
        } else if (SAMPLE_PRIMARY_REPO.equalsIgnoreCase(curationRepo)) {
            staticView = StaticView.SAMPLES_DYNAMIC;
        } else if (SAMPLE_CURATED_REPO.equalsIgnoreCase(curationRepo)) {
            staticView = StaticView.SAMPLES_CURATED;
        } else {
            staticView = StaticView.SAMPLES_CURATED;
        }

        return staticView;
    }

    public enum StaticView {
        SAMPLES_CURATED(StaticViewWrapper.SAMPLE_CURATED_REPO, StaticViewWrapper.SAMPLE_CURATED_REPO_MONGO),
        SAMPLES_DYNAMIC(StaticViewWrapper.SAMPLE_PRIMARY_REPO, StaticViewWrapper.SAMPLE_PRIMARY_REPO_MONGO);

        private String collectionName;
        private String staticViewName;

        StaticView(String staticViewName, String collectionName) {
            this.collectionName = collectionName;
            this.staticViewName = staticViewName;
        }

        public String getCollectionName() {
            return collectionName;
        }

        public String getCurationRepositoryName() {
            return staticViewName;
        }
    }
}
