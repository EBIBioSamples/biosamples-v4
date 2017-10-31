package uk.ac.ebi.biosamples.model.filter;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum FilterType {
    ATTRIBUTE_FILTER("fa", AttributeFilter.Builder.class),
    NAME_FILTER("fn", NameFilter.Builder.class),
    RELATION_FILER("fr", RelationFilter.Builder.class),
    INVERSE_RELATION_FILTER("fir", InverseRelationFilter.Builder.class),
    DOMAIN_FILTER("fdom", DomainFilter.Builder.class),
    DATE_FILTER("fdt", DateRangeFilter.Builder.class),
    EXTERNAL_REFERENCE_DATA_FILTER("ferd", ExternalReferenceDataFilter.Builder.class),
    ACCESSION_FILTER("facc", AccessionFilter.Builder.class);

    private static List<FilterType> filterTypesByLength = new ArrayList<>();

    static {
        filterTypesByLength = Stream.of(values())
                .sorted(
                    Comparator.comparingInt((FilterType f) -> f.getSerialization().length()).reversed()
                    .thenComparing((FilterType::getSerialization))
                ).collect(Collectors.toList());
    }

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
        for(FilterType type: filterTypesByLength) {
            if (filterString.startsWith(type.getSerialization())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Cannot infer filter type from string " + filterString);
    }

}
