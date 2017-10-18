package uk.ac.ebi.biosamples.model.filters;

import org.springframework.stereotype.Service;

@Service
public class FilterBuilder {
    public static ValueFilterBuilder getFilterBuilderForAttributeField(String label) {
        return new ValueFilterBuilder(FilterType.ATTRIBUTE_FILTER, label);
    }

    public static ValueFilterBuilder getFilterBuilderForRelationField(String label) {
        return new ValueFilterBuilder(FilterType.RELATION_FILER, label);
    }

    public static ValueFilterBuilder getFilterBuilderForInverseRelationField(String label) {
        return new ValueFilterBuilder(FilterType.INVERSE_RELATION_FILTER, label);
    }

    public static DateRangeFilterBuilder getFilterBuilderForDateRangeField(String fieldLabel) {
        return new DateRangeFilterBuilder(fieldLabel);
    }

    public static DateRangeFilterBuilder releaseDateFilterBuilder() {
        return new DateRangeFilterBuilder("release_dt");
    }

    public static DateRangeFilterBuilder updateDateFilterBuilder() {
        return new DateRangeFilterBuilder("update_dt");
    }

    public static Filter buildFromString(String serializedFilter) {
        return new StringFilterBuilder(serializedFilter).build();
    }


}
