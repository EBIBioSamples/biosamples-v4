package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filters.*;

@Service
public class FilterFactory {
    public static AttributeFilter.Builder onAttribute(String label) {
        return new AttributeFilter.Builder(label);
    }

    public static RelationFilter.Builder onRelation(String label) {
        return new RelationFilter.Builder(label);
    }

    public static InverseRelationFilter.Builder onInverseRelation(String label) {
        return new InverseRelationFilter.Builder(label);
    }

    public static DateRangeFilter.Builder onDate(String fieldLabel) {
        return new DateRangeFilter.Builder(fieldLabel);
    }

    public static DateRangeFilter.Builder onReleaseDate() {
        return new DateRangeFilter.Builder("release");
    }

    public static DateRangeFilter.Builder onUpdateDate() {
        return new DateRangeFilter.Builder("update");
    }

    public static Filter buildFromString(String serializedFilter) {
        FilterType filterType = FilterType.ofFilterString(serializedFilter);
        String filterLabelAndValueSerialization = serializedFilter.replace(filterType.getSerialization() + ":", "");

        if (!serializedFilter.contains(":")) {
            return filterType.getBuilderForLabel(filterLabelAndValueSerialization).build().getFieldPresenceFilter();
        } else {
            String[] valueElements = filterLabelAndValueSerialization.split(":", 2);
            String filterLabel = valueElements[0];
            String filterValue = valueElements[1];
            return filterType.getBuilderForLabel(filterLabel).parseValue(filterValue).build();
        }
    }


}
