package uk.ac.ebi.biosamples.service.ga4ghService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.model.ga4ghmetadata.SearchingForm;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class FilterCreator {

    private FormFilterCreator filterCreatorByForm;
    private GA4GHFilterCreator filterCreatorGA4GH;

    @Autowired
    FilterCreator(FormFilterCreator filterCreatorByForm, GA4GHFilterCreator filterCreatorGA4GH) {
        this.filterCreatorByForm = filterCreatorByForm;
        this.filterCreatorGA4GH = filterCreatorGA4GH;
    }

    public Collection<Collection<Filter>> createFilters(SearchingForm form) {
        Collection<Collection<Filter>> filters = new ArrayList<>();
        Collection<Filter> formFilters = filterCreatorByForm.getFilters(form);
        Collection<Collection<Filter>> ga4ghFilters = filterCreatorGA4GH.getFilters();
        for (Collection<Filter> ga4ghFilterItem : ga4ghFilters) {
            ga4ghFilterItem.addAll(formFilters);
            filters.add(ga4ghFilterItem);
        }
        return filters;

    }
}
