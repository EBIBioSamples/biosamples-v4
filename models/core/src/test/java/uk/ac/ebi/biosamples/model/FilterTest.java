package uk.ac.ebi.biosamples.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.biosamples.model.filters.*;
import uk.ac.ebi.biosamples.service.FilterBuilder;

import java.time.*;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {
        FilterBuilder.class
})
public class FilterTest {

    @Autowired
    public FilterBuilder filterBuilder;

    @Test
    public void testAttributeFilterDeserialization() {
        String stringToTest = "fa:organism:Homo sapiens";
        Filter expectedFilter = FilterBuilder.create().onAttribute("organism").withValue("Homo sapiens").build();

        Filter attributeFilter = FilterBuilder.create().buildFromString(stringToTest);
        assertEquals(attributeFilter, expectedFilter);
    }

    @Test
    public void testFromLocalDateFilterDeserialization() {
        String stringToTest = "fdt:update:from=2017-01-10";
        ZonedDateTime from = ZonedDateTime.of( 2017, 1, 10, 0, 0, 0, 0, ZoneId.of("UTC"));

        Filter expectedFilter = FilterBuilder.create().onUpdateDate().from(from).build();
        Filter dateRangeFilter = FilterBuilder.create().buildFromString(stringToTest);
        assertEquals(dateRangeFilter, expectedFilter);
    }

    @Test
    public void testDateRangeFromLocalDateTimeToLocalDateFilterDeserialization() {
        String stringToTest = "fdt:release:from=2014-01-01T20:30:00until=2015-01-01";
        ZonedDateTime from = ZonedDateTime.of( 2014, 1, 1, 20, 30, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime to = ZonedDateTime.of( 2015, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

        Filter expectedFilter = FilterBuilder.create().onReleaseDate().from(from).until(to).build();
        assertEquals(FilterBuilder.create().buildFromString(stringToTest), expectedFilter);
    }

    @Test
    public void testDateRangeWithTimeZoneFilterDeserialization() {
        String stringToTest = "fdt:update:until=2016-01-01T23:00:00Z[CET]";
        ZonedDateTime to = ZonedDateTime.of(2016,1,1,23,0,0,0,ZoneId.of("CET"));
        Filter expectedFilter = FilterBuilder.create().onUpdateDate().until(to).build();
        Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
        assertEquals(actualFilter, expectedFilter);
    }

    @Test
    public void testDateRangeWithOffsetFilterDeserialization() {
        String stringToTest = "fdt:update:until=2016-01-01T23:00:00+01:00";
        ZonedDateTime to = ZonedDateTime.of(2016,1,1,23,0,0,0, ZoneOffset.of("+01:00"));
        Filter expectedFilter = FilterBuilder.create().onUpdateDate().until(to).build();
        Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
        assertEquals(actualFilter, expectedFilter);
    }


    @Test
    public void testInvertedDateRangeFilterDeserialization() {
       String stringToTest = "fdt:update:until=2018-01-01from=2016-01-01";
       ZonedDateTime from = ZonedDateTime.of(LocalDate.of(2016,1,1), LocalTime.MIDNIGHT, ZoneId.of("UTC"));
       ZonedDateTime to = ZonedDateTime.of(LocalDate.of(2018,1,1), LocalTime.MIDNIGHT, ZoneId.of("UTC"));
       Filter expectedFilter = FilterBuilder.create().onUpdateDate().from(from).until(to).build();
       Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
       assertEquals(actualFilter, expectedFilter);
    }

}
