package uk.ac.ebi.biosamples.service;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.biosamples.model.filter.Filter;

import java.util.Optional;

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
        Optional<Filter> parsedFilter = queryParser.getDateFiltersFromQuery(queryWithDateFilter);
        Filter expectedFilter = FilterBuilder.create().onUpdateDate().from("2017-01-01").until("2017-01-01").build();
        Assertions.assertThat(parsedFilter.get()).isEqualTo(expectedFilter);
    }

    @Test
    public void itShoudlKeepQueryAsItIs() {
        String queryWithDateFiltersAndRegularText = "test updatedate:[2017-01-01 TO 2018-01-01] releasedate:[2018-01-01 TO 2018-01-01]";
        Optional<Filter> parsedFilter = queryParser.getDateFiltersFromQuery(queryWithDateFiltersAndRegularText);
        String cleanQuery = queryParser.cleanQueryFromDateFilters(queryWithDateFiltersAndRegularText);
        Optional<Filter> expectedFilters = Optional.empty();
        Assertions.assertThat(parsedFilter).isEqualTo(expectedFilters);
        Assertions.assertThat(cleanQuery).isEqualToIgnoringWhitespace(queryWithDateFiltersAndRegularText);
    }

    @Test
    public void itShouldCleanTheQueryParameterAndExtractAFilter() {
        String queryWithDateFilter = "releasedate:[2010-01-01 TO 2017-01-01]";
        Optional<Filter> parsedFilter = queryParser.getDateFiltersFromQuery(queryWithDateFilter);
        String cleanQueryParameter = queryParser.cleanQueryFromDateFilters(queryWithDateFilter);
        Filter expectedFilter = FilterBuilder.create().onReleaseDate().from("2010-01-01").until("2017-01-01").build();

        Assertions.assertThat(parsedFilter).isEqualTo(Optional.of(expectedFilter));
        Assertions.assertThat(cleanQueryParameter).isEqualTo("*");
    }

    @Test
    public void itShouldBeAbleToReadRangeWithEncodedSpaces() {
        String queryWithDateFilter = "releasedate:[2010-01-01%20TO%202017-01-01]";
        Optional<Filter> parsedFilter = queryParser.getDateFiltersFromQuery(queryWithDateFilter);
        String cleanQueryParameter = queryParser.cleanQueryFromDateFilters(queryWithDateFilter);
        Filter expectedFilter = FilterBuilder.create().onReleaseDate().from("2010-01-01").until("2017-01-01").build();

        Assertions.assertThat(parsedFilter).isEqualTo(Optional.of(expectedFilter));
        Assertions.assertThat(cleanQueryParameter).isEqualTo("*");

    }
}
