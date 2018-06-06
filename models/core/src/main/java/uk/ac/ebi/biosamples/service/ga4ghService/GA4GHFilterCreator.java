package uk.ac.ebi.biosamples.service.ga4ghService;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Service
class GA4GHFilterCreator {

    private FilterBuilder builder;
    private Collection<Collection<Filter>> filters;
    private final String externalReference = "ENA";
    private final List<String> attributeLabels = Arrays.asList("Organism", "organism");
    private final List<String> values = Arrays.asList("Homo sapiens", "homo sapiens");

    GA4GHFilterCreator() {
        builder = FilterBuilder.create();
        filters = new LinkedList<>();
        createFilters();
    }

    Collection<Collection<Filter>> getFilters() {
        return filters;
    }

    private void createFilters() {
        for (String attribute : attributeLabels) {
            for (String value : values) {
                List<Filter> tempFilters = new LinkedList<>();
                Filter attrbuteFilter = builder
                        .onAttribute(attribute)
                        .withValue(value)
                        .build();
                Filter externalReferenceFilter = builder
                        .onDataFromExternalReference(externalReference)
                        .build();
                tempFilters.add(attrbuteFilter);
                tempFilters.add(externalReferenceFilter);
                filters.add(tempFilters);
            }

        }

    }

}

