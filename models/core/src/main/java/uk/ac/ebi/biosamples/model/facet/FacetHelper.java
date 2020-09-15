package uk.ac.ebi.biosamples.model.facet;

import java.util.List;

public class FacetHelper {
    private FacetHelper() {
        //hide constructor
    }

    public static final List<String> FACETING_FIELDS = List.of("organism", "external reference", "sex", "tissue", "strain", "organism part",
            "cell type", "isolate", "sample type", "genotype", "isolation source", "histological type", "age", "host",
            "latitude and longitude", "environmental medium", "biomaterial provider", "development stage",
            "investigation type", "disease state", "cell line", "treatment", "depth", "host sex", "cultivar",
            "elevation", "host disease", "developmental stage", "disease", "host age", "phenotype", "breed",
            "collection date", "geographic location", "data use conditions");
    public static final List<String> EX_REF_FACETING_FIELDS = List.of("external reference", "data use conditions");
    public static final List<String> RANGE_FACETING_FIELDS = List.of("release_dt"); // we are only supporting date range facets now

    public static String get_encoding_suffix(String attribute) {
        return EX_REF_FACETING_FIELDS.contains(attribute) ? "_av_ss" : "_av_ss";
    }

    public static int compareFacets(String f1, String f2) {
        if (!FACETING_FIELDS.contains(f1) && !FACETING_FIELDS.contains(f2)) {
            return 0;
        } else if (!FACETING_FIELDS.contains(f1)) {
            return -1;
        } else if (!FACETING_FIELDS.contains(f2)) {
            return 1;
        }

        return FACETING_FIELDS.indexOf(f2) - FACETING_FIELDS.indexOf(f1);
    }
}
