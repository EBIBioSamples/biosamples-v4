package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filters.*;

@Service
public class FilterBuilder {
    public AttributeFilter.Builder onAttribute(String label) {
        return new AttributeFilter.Builder(label);
    }

    public RelationFilter.Builder onRelation(String label) {
        return new RelationFilter.Builder(label);
    }

    public InverseRelationFilter.Builder onInverseRelation(String label) {
        return new InverseRelationFilter.Builder(label);
    }

    public DateRangeFilter.Builder onDate(String fieldLabel) {
        return new DateRangeFilter.Builder(fieldLabel);
    }

    public DateRangeFilter.Builder onReleaseDate() {
        return new DateRangeFilter.Builder("release");
    }

    public DateRangeFilter.Builder onUpdateDate() {
        return new DateRangeFilter.Builder("update");
    }

    public  Filter buildFromString(String serializedFilter) {
        FilterType filterType = FilterType.ofFilterString(serializedFilter);
        String filterLabelAndValueSerialization = serializedFilter.replace(filterType.getSerialization() + ":", "");

        if (!filterLabelAndValueSerialization.contains(":")) {
            return filterType.getBuilderForLabel(filterLabelAndValueSerialization).build();
        } else {
            String[] valueElements = filterLabelAndValueSerialization.split(":", 2);
            String filterLabel = valueElements[0];
            String filterValue = valueElements[1];
            return filterType.getBuilderForLabel(filterLabel).parseContent(filterValue).build();
        }
    }

    public static FilterBuilder create() {
        return new FilterBuilder();
    }


}
