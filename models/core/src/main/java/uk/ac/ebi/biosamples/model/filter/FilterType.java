package uk.ac.ebi.biosamples.model.filter;

import java.lang.reflect.InvocationTargetException;

public enum FilterType {
    ATTRIBUTE_FILTER("fa", AttributeFilter.Builder.class),
    NAME_FILTER("fn", NameFilter.Builder.class),
    RELATION_FILER("fr", RelationFilter.Builder.class),
    INVERSE_RELATION_FILTER("fir", InverseRelationFilter.Builder.class),
    DOMAIN_FILTER("fdom", DomainFilter.Builder.class),
    DATE_FILTER("fdt", DateRangeFilter.Builder.class),
    EXTERNAL_REFERENCE_DATA_FILTER("ferd", ExternalReferenceDataFilter.Builder.class);

    String serialization;
    Class<? extends Filter.Builder> associatedBuilder;

    FilterType(String serialization, Class<? extends Filter.Builder> associatedBuilder) {
        this.serialization = serialization;
        this.associatedBuilder = associatedBuilder;
    }

    public String getSerialization() {
        return this.serialization;
    }

    public Filter.Builder getBuilderForLabel(String label) {
        try {
			return this.associatedBuilder.getConstructor(String.class).newInstance(label);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("FilterType " + this + " does not provide a proper builder");
        }
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
