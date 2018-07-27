package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;

import java.util.*;

/**
 * GA4GHFilterCreator is a class for creating specific GA4GH filters for samples querying, where Organism is equals to homo sapiens
 * and any interpretation of that, has exterenal reference to ENA.
 *
 * @author Dilshat Salikhov
 */
@Service
public class GA4GHFilterBuilder {

    private FilterBuilder builder;
    private Collection<Filter> filters;
    private final String externalReference = "ENA";
    private final String attributeLabel = "Organism";
    private final List<String> values = Arrays.asList("Homo sapiens", "homo sapiens", "9606", "human", "Human");

    public GA4GHFilterBuilder() {
        builder = FilterBuilder.create();
        filters = new LinkedList<>();
        createFilters();
    }

    public Collection<Filter> getFilters() {
        return filters;
    }

    private void createFilters() {
        for (String value : values) {
            Filter attrbuteFilter = builder
                    .onAttribute(attributeLabel)
                    .withValue(value)
                    .build();
            filters.add(attrbuteFilter);
        }
        Filter externalReferenceFilter = builder
                .onDataFromExternalReference(externalReference)
                .build();
        filters.add(externalReferenceFilter);
    }

}

