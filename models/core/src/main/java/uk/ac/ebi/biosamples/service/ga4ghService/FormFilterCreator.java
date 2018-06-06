package uk.ac.ebi.biosamples.service.ga4gh_services;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;
import uk.ac.ebi.biosamples.utils.ga4gh_utils.SearchingForm;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

@Service
class FormFilterCreator {
    private FilterBuilder builder;
    private Collection<Filter> filters;

    FormFilterCreator() {
        builder = FilterBuilder.create();
        filters = new ArrayList<>();
    }

    Collection<Filter> getFilters(SearchingForm form) {
        filters.add(createReleaseDateFilters(form.getReleaseDateFrom(), form.getReleaseDateUntil()));
        return filters;
    }

    private Filter createReleaseDateFilters(Date from, Date until) {
        return builder
                .onReleaseDate()
                .from(dateFormatter(from))
                .until(dateFormatter(until))
                .build();
    }

    private String dateFormatter(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd");
        return formatter.format(date);
    }
}
