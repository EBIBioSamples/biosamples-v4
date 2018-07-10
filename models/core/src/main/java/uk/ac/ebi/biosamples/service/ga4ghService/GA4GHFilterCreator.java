package uk.ac.ebi.biosamples.service.ga4ghService;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;

import java.util.*;

/**
 * GA4GHFilterCreator is a class for creating specific GA4GH filters for samples querying, where Organism is equals to homo sapiens
 * and any interpretation of that, has exterenal reference to ENA.
 *
 * @author  Dilshat Salikhov
 */
@Service
public class GA4GHFilterCreator {

    private FilterBuilder builder;
    private Collection<Collection<Filter>> filters;
    private final String externalReference = "ENA";
    private final List<String> attributeLabels = Arrays.asList("Organism", "organism");
    private final List<String> values = Arrays.asList("Homo sapiens", "homo sapiens","9606","human","Human");

    GA4GHFilterCreator() {
        builder = FilterBuilder.create();
        filters = new LinkedList<>();
        createFilters();
    }

    public Collection<Collection<Filter>> getFilters() {
        return filters;
    }

    private void createFilters() {
        for (String attribute : attributeLabels) {
            Collection<Filter> organismSetOfFilters = new ArrayList<>();
            for (String value : values) {
                Filter attrbuteFilter = builder
                        .onAttribute(attribute)
                        .withValue(value)
                        .build();
                organismSetOfFilters.add(attrbuteFilter);
            }
            Filter externalReferenceFilter = builder
                    .onDataFromExternalReference(externalReference)
                    .build();
            organismSetOfFilters.add(externalReferenceFilter);
            filters.add(organismSetOfFilters);
        }

    }

}

