package uk.ac.ebi.biosamples.service;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.biosamples.legacy.xml.service.LegacyQueryParser;
import uk.ac.ebi.biosamples.model.filter.Filter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
public class QueryParserTest {

    public LegacyQueryParser queryParser = new LegacyQueryParser();

    @Test
    public void itShouldNotFindDateFiltersInBaseQuery(){
        String queryString = "test";
        boolean containsDateRangeFilter = queryParser.queryContainsDateRangeFilter(queryString);
        assertThat(containsDateRangeFilter).isFalse();
    }

    @Test
    public void itShouldFindDateDateFiltersInDateFilterQuery() {
        String queryWithDateFilter = "updatedate:[2017-01-01 TO 2017-01-01]";
        boolean containsDateRangeFilter = queryParser.queryContainsDateRangeFilter(queryWithDateFilter);
        assertThat(containsDateRangeFilter).isTrue();
    }

    @Test
    public void itShouldReturnAFilterFromDateRangeFilterQuery() {
        String queryWithDateFilter = "updatedate:[2017-01-01 TO 2017-01-01]";
        Optional<Filter> parsedFilter = queryParser.extractDateFilterFromQuery(queryWithDateFilter);
        Filter expectedFilter = FilterBuilder.create().onUpdateDate().from("2017-01-01").until("2017-01-01").build();
        assertThat(parsedFilter.get()).isEqualTo(expectedFilter);
    }

    @Test
    @Ignore
    public void itShoudlKeepQueryAsItIs() {
        String queryWithDateFiltersAndRegularText = "test updatedate:[2017-01-01 TO 2018-01-01] releasedate:[2018-01-01 TO 2018-01-01]";
        Optional<Filter> parsedFilter = queryParser.extractDateFilterFromQuery(queryWithDateFiltersAndRegularText);
        String cleanQuery = queryParser.cleanQueryFromDateFilters(queryWithDateFiltersAndRegularText);
        Optional<Filter> expectedFilters = Optional.empty();
        assertThat(parsedFilter).isEqualTo(expectedFilters);
        assertThat(cleanQuery).isEqualToIgnoringWhitespace(queryWithDateFiltersAndRegularText);
    }

    @Test
    public void itShouldCleanTheQueryParameterAndExtractAFilter() {
        String queryWithDateFilter = "releasedate:[2010-01-01 TO 2017-01-01]";
        Optional<Filter> parsedFilter = queryParser.extractDateFilterFromQuery(queryWithDateFilter);
        String cleanQueryParameter = queryParser.cleanQueryFromDateFilters(queryWithDateFilter);
        Filter expectedFilter = FilterBuilder.create().onReleaseDate().from("2010-01-01").until("2017-01-01").build();

        assertThat(parsedFilter).isEqualTo(Optional.of(expectedFilter));
        assertThat(cleanQueryParameter).isEqualTo("*:*");
    }

    @Test
    public void itShouldBeAbleToReadRangeWithEncodedSpaces() {
        String queryWithDateFilter = "releasedate:[2010-01-01%20TO%202017-01-01]";
        Optional<Filter> parsedFilter = queryParser.extractDateFilterFromQuery(queryWithDateFilter);
        String cleanQueryParameter = queryParser.cleanQueryFromDateFilters(queryWithDateFilter);
        Filter expectedFilter = FilterBuilder.create().onReleaseDate().from("2010-01-01").until("2017-01-01").build();

        assertThat(parsedFilter).isEqualTo(Optional.of(expectedFilter));
        assertThat(cleanQueryParameter).isEqualTo("*:*");

    }

    @Test
    public void itShouldBeAbleToReadSampleAccessionAfterDateRange() {
        String queryWithDateFilterAndSampleAccession = "updatedate:[2018-01-01 TO 2018-01-01] AND SAMEA*";
        Optional<Filter> dateRangeFilter = queryParser.extractDateFilterFromQuery(queryWithDateFilterAndSampleAccession);
        Optional<Filter> accessionFilter = queryParser.extractAccessionFilterFromQuery(queryWithDateFilterAndSampleAccession);
        Filter expectedDateRangeFilter = FilterBuilder.create().onUpdateDate().from("2018-01-01").until("2018-01-01").build();
        Filter expectedAccessionFilter = FilterBuilder.create().onAccession("SAMEA.*").build();

        assertThat(dateRangeFilter).isEqualTo(Optional.of(expectedDateRangeFilter));
        assertThat(accessionFilter).isEqualTo(Optional.of(expectedAccessionFilter));
    }

    @Test
    public void itShouldBeAbleToReadFiltersEvenIfOrderIsInverted() {
        String queryWithDateFilterAndSampleAccession = "SAMEA* AND updatedate:[2018-01-01 TO 2018-01-01]";
        Optional<Filter> dateRangeFilter = queryParser.extractDateFilterFromQuery(queryWithDateFilterAndSampleAccession);
        Optional<Filter> accessionFilter = queryParser.extractAccessionFilterFromQuery(queryWithDateFilterAndSampleAccession);
        Filter expectedDateRangeFilter = FilterBuilder.create().onUpdateDate().from("2018-01-01").until("2018-01-01").build();
        Filter expectedAccessionFilter = FilterBuilder.create().onAccession("SAMEA.*").build();

        assertThat(dateRangeFilter).isEqualTo(Optional.of(expectedDateRangeFilter));
        assertThat(accessionFilter).isEqualTo(Optional.of(expectedAccessionFilter));

    }

    @Test
    public void itShouldCleanTheQueryFromAllKnowFilters() {
        String queryWithDateFilterAndSampleAccession = "updatedate:[2018-01-01 TO 2018-01-01] AND SAMEA*";
        String finalQuery = queryParser.cleanQueryFromKnownFilters(queryWithDateFilterAndSampleAccession);

        assertThat(finalQuery).isEqualTo("*:*");

    }

    @Test
    public void itShouldCleanTheQueryEvenWithEncodedSpaces() {
        String queryWithDateFilterAndSampleAccession = "updatedate:[2018-01-01%20TO%202018-01-01]%20AND%20SAMEA*";
        String finalQuery = queryParser.cleanQueryFromKnownFilters(queryWithDateFilterAndSampleAccession);

        assertThat(finalQuery).isEqualTo("*:*");

    }
}
