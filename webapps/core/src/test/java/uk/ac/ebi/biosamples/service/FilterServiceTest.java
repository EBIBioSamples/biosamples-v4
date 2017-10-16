package uk.ac.ebi.biosamples.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.filters.Filter;
import uk.ac.ebi.biosamples.model.filters.FilterType;
import uk.ac.ebi.biosamples.model.filters.ValueFilter;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        FilterService.class
})
@EnableAutoConfiguration
@TestPropertySource(properties={"aap.domains.url=''"})
public class FilterServiceTest {

    @Autowired
    public FilterService filterService;

    @Test
    public void testAttributeFilterDeserialization() {
        String[] stringToTest = {"fa:organism:Homo sapiens"};
        Filter expectedFilter = new Filter(FilterType.ATTRIBUTE_FILTER, "organism",
                new ValueFilter(Collections.singletonList("Homo sapiens")));

        Collection<Filter> filters = filterService.getFiltersCollection(stringToTest);
        assertEquals(filters.size(), 1);
        Filter attributeFilter = filters.iterator().next();
        assertEquals(attributeFilter, expectedFilter);
    }

}
