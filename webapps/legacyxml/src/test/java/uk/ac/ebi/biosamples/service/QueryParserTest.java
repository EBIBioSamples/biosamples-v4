package uk.ac.ebi.biosamples.service;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.biosamples.model.filter.Filter;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
public class QueryParserTest {

    public LegacyQueryParser queryParser = new LegacyQueryParser();

    @Test
    public void itShouldNotFindDateFiltersInBaseQuery(){
        String queryString = "test";
        boolean containsDateRangeFilter = queryParser.checkQueryContainsDateFilters(queryString);
        Assertions.assertThat(containsDateRangeFilter).isFalse();
    }

    @Test
    public void itShouldFindDateDateFiltersInDateFilterQuery() {
        String queryWithDateFilter = "updatedate:[2017-01-01 TO 2017-01-01]";
        boolean containsDateRangeFilter = queryParser.checkQueryContainsDateFilters(queryWithDateFilter);
        Assertions.assertThat(containsDateRangeFilter).isTrue();
    }

    @Test
    public void itShouldReturnAFilterFromDateRangeFilterQuery() {
        String queryWithDateFilter = "updatedate:[2017-01-01 TO 2017-01-01]";
        Filter parsedFilter = queryParser.getDateFiltersFromQuery(queryWithDateFilter).get(0);
        Filter expectedFilter = FilterBuilder.create().onUpdateDate().from("2017-01-01").until("2017-01-01").build();
        Assertions.assertThat(parsedFilter).isEqualTo(expectedFilter);
    }

    @Test
    public void itShouldReturnFiltersAndCleanTheQuery() {
        String queryWithDateFiltersAndRegularText = "test updatedate:[2017-01-01 TO 2018-01-01] releasedate:[2018-01-01 TO 2018-01-01]";
        List<Filter> parsedFilters = queryParser.getDateFiltersFromQuery(queryWithDateFiltersAndRegularText);
        String cleanQuery = queryParser.cleanQueryFromDateFilters(queryWithDateFiltersAndRegularText);
        List<Filter> expectedFilters = Arrays.asList(
                FilterBuilder.create().onUpdateDate().from("2017-01-01").until("2018-01-01").build(),
                FilterBuilder.create().onReleaseDate().from("2018-01-01").until("2018-01-01").build()
        );
        Assertions.assertThat(parsedFilters).containsAll(expectedFilters);
        Assertions.assertThat(cleanQuery).isEqualToIgnoringWhitespace("test");

    }
}
