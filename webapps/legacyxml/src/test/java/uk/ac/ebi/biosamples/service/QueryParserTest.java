/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.biosamples.legacy.xml.service.LegacyQueryParser;
import uk.ac.ebi.biosamples.model.filter.Filter;

@RunWith(SpringJUnit4ClassRunner.class)
public class QueryParserTest {

  public LegacyQueryParser queryParser = new LegacyQueryParser();

  @Test
  public void itShouldNotFindDateFiltersInBaseQuery() {
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
    Filter expectedFilter =
        FilterBuilder.create().onUpdateDate().from("2017-01-01").until("2017-01-01").build();
    assertThat(parsedFilter.get()).isEqualTo(expectedFilter);
  }

  @Test
  @Ignore
  public void itShoudlKeepQueryAsItIs() {
    String queryWithDateFiltersAndRegularText =
        "test updatedate:[2017-01-01 TO 2018-01-01] releasedate:[2018-01-01 TO 2018-01-01]";
    Optional<Filter> parsedFilter =
        queryParser.extractDateFilterFromQuery(queryWithDateFiltersAndRegularText);
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
    Filter expectedFilter =
        FilterBuilder.create().onReleaseDate().from("2010-01-01").until("2017-01-01").build();

    assertThat(parsedFilter).isEqualTo(Optional.of(expectedFilter));
    assertThat(cleanQueryParameter).isEqualTo("*:*");
  }

  @Test
  public void itShouldBeAbleToReadRangeWithEncodedSpaces() {
    String queryWithDateFilter = "releasedate:[2010-01-01%20TO%202017-01-01]";
    Optional<Filter> parsedFilter = queryParser.extractDateFilterFromQuery(queryWithDateFilter);
    String cleanQueryParameter = queryParser.cleanQueryFromDateFilters(queryWithDateFilter);
    Filter expectedFilter =
        FilterBuilder.create().onReleaseDate().from("2010-01-01").until("2017-01-01").build();

    assertThat(parsedFilter).isEqualTo(Optional.of(expectedFilter));
    assertThat(cleanQueryParameter).isEqualTo("*:*");
  }

  @Test
  public void itShouldBeAbleToReadSampleAccessionAfterDateRange() {
    String queryWithDateFilterAndSampleAccession =
        "updatedate:[2018-01-01 TO 2018-01-01] AND SAMEA*";
    Optional<Filter> dateRangeFilter =
        queryParser.extractDateFilterFromQuery(queryWithDateFilterAndSampleAccession);
    Optional<Filter> accessionFilter =
        queryParser.extractAccessionFilterFromQuery(queryWithDateFilterAndSampleAccession);
    Filter expectedDateRangeFilter =
        FilterBuilder.create().onUpdateDate().from("2018-01-01").until("2018-01-01").build();
    Filter expectedAccessionFilter = FilterBuilder.create().onAccession("SAMEA.*").build();

    assertThat(dateRangeFilter).isEqualTo(Optional.of(expectedDateRangeFilter));
    assertThat(accessionFilter).isEqualTo(Optional.of(expectedAccessionFilter));
  }

  @Test
  public void itShouldBeAbleToReadFiltersEvenIfOrderIsInverted() {
    String queryWithDateFilterAndSampleAccession =
        "SAMEA* AND updatedate:[2018-01-01 TO 2018-01-01]";
    Optional<Filter> dateRangeFilter =
        queryParser.extractDateFilterFromQuery(queryWithDateFilterAndSampleAccession);
    Optional<Filter> accessionFilter =
        queryParser.extractAccessionFilterFromQuery(queryWithDateFilterAndSampleAccession);
    Filter expectedDateRangeFilter =
        FilterBuilder.create().onUpdateDate().from("2018-01-01").until("2018-01-01").build();
    Filter expectedAccessionFilter = FilterBuilder.create().onAccession("SAMEA.*").build();

    assertThat(dateRangeFilter).isEqualTo(Optional.of(expectedDateRangeFilter));
    assertThat(accessionFilter).isEqualTo(Optional.of(expectedAccessionFilter));
  }

  @Test
  public void itShouldCleanTheQueryFromAllKnowFilters() {
    String queryWithDateFilterAndSampleAccession =
        "updatedate:[2018-01-01 TO 2018-01-01] AND SAMEA*";
    String finalQuery =
        queryParser.cleanQueryFromKnownFilters(queryWithDateFilterAndSampleAccession);

    assertThat(finalQuery).isEqualTo("*:*");
  }

  @Test
  public void itShouldCleanTheQueryEvenWithEncodedSpaces() {
    String queryWithDateFilterAndSampleAccession =
        "updatedate:[2018-01-01%20TO%202018-01-01]%20AND%20SAMEA*";
    String finalQuery =
        queryParser.cleanQueryFromKnownFilters(queryWithDateFilterAndSampleAccession);

    assertThat(finalQuery).isEqualTo("*:*");
  }
}
