package uk.ac.ebi.biosamples.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {
        FilterBuilder.class
})
public class FilterSerializationTest {

    @Autowired
    public FilterBuilder filterBuilder;

    @Test
    public void testAttributeFilterDeserialization() {
        String stringToTest = "attr:organism:Homo sapiens";
        Filter expectedFilter = FilterBuilder.create().onAttribute("organism").withValue("Homo sapiens").build();

        Filter attributeFilter = FilterBuilder.create().buildFromString(stringToTest);
        assertEquals(attributeFilter, expectedFilter);
    }

    @Test
    public void testFromLocalDateFilterDeserialization() {
        String stringToTest = "dt:update:from=2017-01-10";
        ZonedDateTime from = ZonedDateTime.of( 2017, 1, 10, 0, 0, 0, 0, ZoneId.of("UTC"));

        Filter expectedFilter = FilterBuilder.create().onUpdateDate().from(from).build();
        Filter dateRangeFilter = FilterBuilder.create().buildFromString(stringToTest);
        assertEquals(dateRangeFilter, expectedFilter);
    }

    @Test
    public void testDateRangeFromLocalDateTimeToLocalDateFilterDeserialization() {
        String stringToTest = "dt:release:from=2014-01-01T20:30:00until=2015-01-01";
        ZonedDateTime from = ZonedDateTime.of( 2014, 1, 1, 20, 30, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime to = ZonedDateTime.of( 2015, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).plusDays(1).minusNanos(1);

        Filter expectedFilter = FilterBuilder.create().onReleaseDate().from(from).until(to).build();
        assertEquals(FilterBuilder.create().buildFromString(stringToTest), expectedFilter);
    }

    @Test
    public void testDateRangeWithTimeZoneFilterDeserialization() {
        String stringToTest = "dt:update:until=2016-01-01T23:00:00Z[CET]";
        ZonedDateTime to = ZonedDateTime.of(2016,1,1,23,0,0,0,ZoneId.of("CET"));
        Filter expectedFilter = FilterBuilder.create().onUpdateDate().until(to).build();
        Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
        assertEquals(actualFilter, expectedFilter);
    }

    @Test
    public void testDateRangeWithOffsetFilterDeserialization() {
        String stringToTest = "dt:update:until=2016-01-01T23:00:00+01:00";
        ZonedDateTime to = ZonedDateTime.of(2016,1,1,23,0,0,0, ZoneOffset.of("+01:00"));
        Filter expectedFilter = FilterBuilder.create().onUpdateDate().until(to).build();
        Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
        assertEquals(actualFilter, expectedFilter);
    }


    @Test
    public void testInvertedDateRangeFilterDeserialization() {
       String stringToTest = "dt:update:until=2018-01-01from=2016-01-01";
       ZonedDateTime from = ZonedDateTime.of(LocalDate.of(2016,1,1), LocalTime.MIDNIGHT, ZoneId.of("UTC"));
       ZonedDateTime to = ZonedDateTime.of(LocalDate.of(2018,1,1), LocalTime.MIDNIGHT, ZoneId.of("UTC")).plusDays(1).minusNanos(1);
       Filter expectedFilter = FilterBuilder.create().onUpdateDate().from(from).until(to).build();
       Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
       assertEquals(actualFilter, expectedFilter);
    }

    @Test
    public void testAccessionFilterSerialization() {
        String stringToTest = "acc:SAMEA123123";
        Filter expectedFilter = FilterBuilder.create().onAccession("SAMEA123123").build();
        Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
        assertEquals(expectedFilter, actualFilter);
    }

    @Test
    public void testExternalReferenceFilterSerialization() {
        String stringToTest = "extd:ENA:E-MTAB-123123";
        Filter expectedFilter = FilterBuilder.create()
                .onDataFromExternalReference("ENA")
                .withValue("E-MTAB-123123").build();
        Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
        assertEquals(expectedFilter, actualFilter);
    }

    @Test
    public void testRelationFilterSerialization() {
        String stringToTest = "rel:derive From:SAMEA123123";
        Filter expectedFilter = FilterBuilder.create()
                .onRelation("derive From")
                .withValue("SAMEA123123")
                .build();
        Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
        assertEquals(expectedFilter, actualFilter);
    }

    @Test
    public void testInverseRelationFilterSerialization() {
        String stringToTest = "rrel:derive From:SAMEA123123";
        Filter expectedFilter = FilterBuilder.create()
                .onInverseRelation("derive From")
                .withValue("SAMEA123123")
                .build();
        Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
        assertEquals(expectedFilter, actualFilter);
    }

    @Test
    public void testNameFilterSerialization() {
        String stringToTest = "name:Test filter 2";
        Filter expectedFilter = FilterBuilder.create()
                .onName("Test filter 2")
                .build();
        Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
        assertEquals(expectedFilter, actualFilter);
    }

    @Test
    public void testWildcardFilterSerialization() {
        String stringToTest = "name:Test filter *";
        Filter expectedFilter = FilterBuilder.create()
                .onName("Test filter *")
                .build();
        Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
        assertEquals(expectedFilter, actualFilter);
    }

    @Test
    public void testEscapeSerialization() {
       String stringToTest = "attr:(?\\:O)organism";
       Filter expectedFilter = FilterBuilder.create().onAttribute("(?:O)organism").build();
       Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
       assertEquals(expectedFilter,actualFilter);
    }

    @Test
    public void testForDifferentFiltersWithEscapedCharacters() {
        String stringToTest = "attr:(?\\:O)rganism";
        Filter wrongDeserializedFilter = FilterBuilder.create().onAttribute("(?").withValue("O)rganism").build();
        Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
        assertNotEquals(wrongDeserializedFilter, actualFilter);

    }

}
