package uk.ac.ebi.biosamples.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.filters.DateRangeFilterContent;
import uk.ac.ebi.biosamples.model.filters.Filter;
import uk.ac.ebi.biosamples.model.filters.FilterType;
import uk.ac.ebi.biosamples.model.filters.ValueFilter;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;
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

    @Test
    public void testFromLocalDateFilterDeserialization() {
       String[] stringToTest = {
               "fdt:update_date:from:2017-01-10"
       };
       ZonedDateTime from = ZonedDateTime.of(
               2017,
               1,
               10,
               0,
               0,
               0,
                0,
                ZoneId.of("UTC"));

       Filter expectedFilter = new Filter(FilterType.DATE_FILTER, "update_date",
               new DateRangeFilterContent(from, null)
       );
       Collection<Filter> filters = filterService.getFiltersCollection(stringToTest);
       assertEquals(filters.size(), 1);
       Filter filter = filters.iterator().next();
       assertEquals(filter, expectedFilter);
    }

    @Test
    public void testDateRangeFromLocalDateTimeToLocalDateFilterDeserialization() {
        String[] stringToTest = {
                "fdt:release_date:from:2014-01-01T20:30:00to:2015-01-01"
        };
        ZonedDateTime from = ZonedDateTime.of( 2014, 1, 1, 20, 30, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime to = ZonedDateTime.of( 2015, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

        Filter expectedFilter = new Filter(FilterType.DATE_FILTER, "release_date",
                new DateRangeFilterContent(from, to)
        );
        Collection<Filter> filters = filterService.getFiltersCollection(stringToTest);
        assertEquals(filters.size(), 1);
        Filter filter = filters.iterator().next();
        assertEquals(filter, expectedFilter);
    }

    @Test
    public void testTwoDifferentDateRangeFilters() {
        String[] stringToTest = {
                "fdt:release_date:from:2014-01-01",
                "fdt:update_date:to:2018-01-01"
        };
        ZonedDateTime releaseFrom = ZonedDateTime.of(LocalDate.of(2014,1, 1), LocalTime.MIDNIGHT, ZoneId.of("UTC"));
        ZonedDateTime updateTo = ZonedDateTime.of(LocalDate.of(2018,1,1), LocalTime.MIDNIGHT, ZoneId.of("UTC"));

        Collection<Filter> expectedFitlers = new ArrayList<>();
        expectedFitlers.add(new Filter(FilterType.DATE_FILTER, "release_date", new DateRangeFilterContent(releaseFrom, null)));
        expectedFitlers.add(new Filter(FilterType.DATE_FILTER, "update_date", new DateRangeFilterContent(null, updateTo)));

        Collection<Filter> filters = filterService.getFiltersCollection(stringToTest);
        assertEquals(filters.size(), 2);
        for (Filter filter: expectedFitlers) {
            filters.remove(filter);
        }
        assertEquals(filters.size(),0);

    }

    @Test
    public void checkDateTimeFormatterFunctionality(){
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(ISO_LOCAL_DATE)
                .optionalStart()           // time made optional
                .appendLiteral('T')
                .append(ISO_LOCAL_TIME)
                .optionalStart()           // zone and offset made optional
                .appendOffsetId()
                .optionalStart()
                .appendLiteral('[')
                .parseCaseSensitive()
                .appendZoneRegionId()
                .appendLiteral(']')
                .optionalEnd()
                .optionalEnd()
                .optionalEnd()
                .toFormatter();
        TemporalAccessor temporalAccessor = formatter.parseBest("2017-10-10",
                ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
        ZonedDateTime temp;
        if (temporalAccessor instanceof ZonedDateTime) {
            temp = (ZonedDateTime) temporalAccessor;
        } else if (temporalAccessor instanceof LocalDateTime) {
            temp = ((LocalDateTime) temporalAccessor).atZone(ZoneId.of("UTC"));
        } else {
            temp = ((LocalDate) temporalAccessor).atStartOfDay(ZoneId.of("UTC"));
        }
        assertEquals(temp.getDayOfMonth(), 10);
        assertEquals(temp.getMonthValue(), 10);
        assertEquals(temp.getYear(), 2017);
    }


}
