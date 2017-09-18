package uk.ac.ebi.biosamples.service;

import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.core.DefaultRelProvider;
import uk.ac.ebi.biosamples.model.facets.Facet;

public class MyCustomRelProvider implements RelProvider {

    private DefaultRelProvider defaultRelProvider = new DefaultRelProvider();

    @Override
    public String getItemResourceRelFor(Class<?> type) {
        if (Facet.class.isAssignableFrom(type)) {
            return "facet";
        }
        return defaultRelProvider.getItemResourceRelFor(type);
    }

    @Override
    public String getCollectionResourceRelFor(Class<?> type) {
        if (Facet.class.isAssignableFrom(type)) {
            return "facets";
        }
        return defaultRelProvider.getCollectionResourceRelFor(type);
    }

    @Override
    public boolean supports(Class<?> delimiter) {
        return defaultRelProvider.supports(delimiter);
    }
}
