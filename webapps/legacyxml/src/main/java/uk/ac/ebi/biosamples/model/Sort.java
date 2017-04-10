package uk.ac.ebi.biosamples.model;

public enum Sort {
    ASCENDING("asc"),
    DESCENDING("desc");

    private String queryParam;

    private Sort(String param) {
        this.queryParam = param;
    }

    public static Sort forParam(String param) {
        switch (param){
            case "asc":
                return ASCENDING;
            default:
                return DESCENDING;
        }
    }
}
