package uk.ac.ebi.biosamples.model.ga4gh_tests;

import org.junit.Test;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;
import uk.ac.ebi.biosamples.service.GA4GHFilterBuilder;

import java.util.Collection;

import static org.junit.Assert.*;

public class GA4GHFilterBuilderTest {

    private GA4GHFilterBuilder filterBuilder;

    public GA4GHFilterBuilderTest(){
        filterBuilder = new GA4GHFilterBuilder();
    }

    @Test
    public void filtersCompletedTest(){
        Collection<Filter> filters = filterBuilder.getFilters();
        FilterBuilder builder = FilterBuilder.create();

        Filter externarReferenceFilter = builder.onDataFromExternalReference("ENA").build();
        assertTrue(filters.contains(externarReferenceFilter));

        Filter organismFilter1 = builder.onAttribute("organism")
                .withValue("Homo sapiens")
                .build();
        assertTrue(filters.contains(organismFilter1));

        Filter organismFilter2 = builder.onAttribute("organism")
                .withValue("homo sapiens")
                .build();
        assertTrue(filters.contains(organismFilter2));

        Filter organismFilter3 = builder.onAttribute("organism")
                .withValue("9606")
                .build();
        assertTrue(filters.contains(organismFilter3));

        Filter organismFilter4 = builder.onAttribute("organism")
                .withValue("human")
                .build();
        assertTrue(filters.contains(organismFilter4));

        Filter organismFilter5 = builder.onAttribute("organism")
                .withValue("Human")
                .build();
        assertTrue(filters.contains(organismFilter5));



    }
}