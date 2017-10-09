package uk.ac.ebi.biosamples.model.filters;

public enum FilterType {
    ATTRIBUTE_FILTER("fa"),
    RELATION_FILER("fr"),
    INVERSE_RELATION_FILTER("fir"),
    DOMAIN_FILTER("fdom"),
    DATE_FILTER("fdt");

//    private static Map<String, FilterType> filterKindMap = new HashMap<>();
//
//    static {
//        for(FilterType type: values()) {
//            filterKindMap.put(type.getKind(), type);
//        }
//    }
    String serialization;

    FilterType(String serialization) {
        this.serialization = serialization;
    }

    public String getSerialization() {
        return this.serialization;
    }

    public static FilterType ofFilterString(String filterString) {
        for(FilterType type: values()) {
            if (filterString.startsWith(type.getSerialization())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Cannot infer filter type from string " + filterString);
    }

}
