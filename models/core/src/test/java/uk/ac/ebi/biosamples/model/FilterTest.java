package uk.ac.ebi.biosamples.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.biosamples.model.filters.*;

import java.time.*;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {
        FilterFactory.class
})
public class FilterTest {

    @Autowired
    public FilterFactory filterFactory;

    @Test
    public void testAttributeFilterDeserialization() {
        String stringToTest = "fa:organism:Homo sapiens";
        Filter expectedFilter = new Filter(FilterType.ATTRIBUTE_FILTER, "organism",
                new ValueFilter("Homo sapiens"));

        Filter attributeFilter = filterFactory.parseFilterFromString(stringToTest);
        assertEquals(attributeFilter, expectedFilter);
    }

    @Test
    public void testFromLocalDateFilterDeserialization() {
        String stringToTest = "fdt:update_date:from=2017-01-10";
        ZonedDateTime from = ZonedDateTime.of( 2017, 1, 10, 0, 0, 0, 0, ZoneId.of("UTC"));

        Filter expectedFilter = new Filter(FilterType.DATE_FILTER, "update_date",
                new DateRangeFilterContent(from, null)
        );
        Filter dateRangeFilter = filterFactory.parseFilterFromString(stringToTest);
        assertEquals(dateRangeFilter, expectedFilter);
    }

    @Test
    public void testDateRangeFromLocalDateTimeToLocalDateFilterDeserialization() {
        String stringToTest = "fdt:release_date:from=2014-01-01T20:30:00to=2015-01-01";
        ZonedDateTime from = ZonedDateTime.of( 2014, 1, 1, 20, 30, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime to = ZonedDateTime.of( 2015, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

        Filter expectedFilter = new Filter(FilterType.DATE_FILTER, "release_date",
                new DateRangeFilterContent(from, to)
        );
        assertEquals(filterFactory.parseFilterFromString(stringToTest), expectedFilter);
    }

    @Test
    public void testDateRangeWithTimeZoneFilterDeserialization() {
        String stringToTest = "fdt:update_date:to=2016-01-01T23:00:00Z[CET]";
        ZonedDateTime to = ZonedDateTime.of(2016,1,1,23,0,0,0,ZoneId.of("CET"));
        Filter expectedFilter = new Filter(FilterType.DATE_FILTER, "update_date", new DateRangeFilterContent(null, to));
        Filter actualFilter = filterFactory.parseFilterFromString(stringToTest);
        assertEquals(actualFilter, expectedFilter);
    }

    @Test
    public void testDateRangeWithOffsetFilterDeserialization() {
        String stringToTest = "fdt:update_date:to=2016-01-01T23:00:00+01:00";
        ZonedDateTime to = ZonedDateTime.of(2016,1,1,23,0,0,0, ZoneOffset.of("+01:00"));
        Filter expectedFilter = new Filter(FilterType.DATE_FILTER, "update_date", new DateRangeFilterContent(null, to));
        Filter actualFilter = filterFactory.parseFilterFromString(stringToTest);
        assertEquals(actualFilter, expectedFilter);
    }


    @Test
    public void testInvertedDateRangeFilterDeserialization() {
       String stringToTest = "fdt:update_date:to=2018-01-01from=2016-01-01";
       ZonedDateTime from = ZonedDateTime.of(LocalDate.of(2016,1,1), LocalTime.MIDNIGHT, ZoneId.of("UTC"));
       ZonedDateTime to = ZonedDateTime.of(LocalDate.of(2018,1,1), LocalTime.MIDNIGHT, ZoneId.of("UTC"));
       Filter expectedFilter = new Filter(FilterType.DATE_FILTER, "update_date", new DateRangeFilterContent(from, to));
       Filter actualFilter = filterFactory.parseFilterFromString(stringToTest);
       assertEquals(actualFilter, expectedFilter);
    }

}
