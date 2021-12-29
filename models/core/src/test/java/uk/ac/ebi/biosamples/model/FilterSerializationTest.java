/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.biosamples.model.filter.Filter;
import uk.ac.ebi.biosamples.service.FilterBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {FilterBuilder.class})
public class FilterSerializationTest {
  private Logger log = LoggerFactory.getLogger(getClass());

  @Autowired public FilterBuilder filterBuilder;

  @Test
  public void testAttributeFilterDeserialization() {
    String stringToTest = "attr:organism:Homo sapiens";
    Filter expectedFilter =
        FilterBuilder.create().onAttribute("organism").withValue("Homo sapiens").build();

    Filter attributeFilter = FilterBuilder.create().buildFromString(stringToTest);
    assertEquals(attributeFilter, expectedFilter);
  }

  @Test
  public void testFromLocalDateFilterDeserialization() {
    String stringToTest = "dt:update:from=2017-01-10";
    Instant from = ZonedDateTime.of(2017, 1, 10, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();

    Filter expectedFilter = FilterBuilder.create().onUpdateDate().from(from).build();
    Filter dateRangeFilter = FilterBuilder.create().buildFromString(stringToTest);
    assertEquals(dateRangeFilter, expectedFilter);
  }

  @Test
  public void testDateRangeFromLocalDateTimeToLocalDateFilterDeserialization() {
    String stringToTest = "dt:release:from=2014-01-01T20:30:00until=2015-01-01";
    Instant from = ZonedDateTime.of(2014, 1, 1, 20, 30, 0, 0, ZoneOffset.UTC).toInstant();
    Instant until = LocalDate.of(2015, 1, 1).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC);
    Filter expectedFilter = FilterBuilder.create().onReleaseDate().from(from).until(until).build();
    Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
    log.info(
        "testDateRangeFromLocalDateTimeToLocalDateFilterDeserialization expected = "
            + expectedFilter.getSerialization());
    log.info(
        "testDateRangeFromLocalDateTimeToLocalDateFilterDeserialization actual = "
            + actualFilter.getSerialization());
    assertEquals(actualFilter, expectedFilter);
  }

  @Test
  public void testInvertedDateRangeFilterDeserialization() {
    String stringToTest = "dt:update:until=2018-01-01from=2016-01-01";
    Instant from = LocalDate.of(2016, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant until = LocalDate.of(2018, 1, 1).atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC);
    Filter expectedFilter = FilterBuilder.create().onUpdateDate().from(from).until(until).build();
    Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
    log.info(
        "testInvertedDateRangeFilterDeserialization expected = "
            + expectedFilter.getSerialization());
    log.info(
        "testInvertedDateRangeFilterDeserialization actual = " + actualFilter.getSerialization());
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
    Filter expectedFilter =
        FilterBuilder.create()
            .onDataFromExternalReference("ENA")
            .withValue("E-MTAB-123123")
            .build();
    Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
    assertEquals(expectedFilter, actualFilter);
  }

  @Test
  public void testRelationFilterSerialization() {
    String stringToTest = "rel:derive From:SAMEA123123";
    Filter expectedFilter =
        FilterBuilder.create().onRelation("derive From").withValue("SAMEA123123").build();
    Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
    assertEquals(expectedFilter, actualFilter);
  }

  @Test
  public void testInverseRelationFilterSerialization() {
    String stringToTest = "rrel:derive From:SAMEA123123";
    Filter expectedFilter =
        FilterBuilder.create().onInverseRelation("derive From").withValue("SAMEA123123").build();
    Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
    assertEquals(expectedFilter, actualFilter);
  }

  @Test
  public void testNameFilterSerialization() {
    String stringToTest = "name:Test filter 2";
    Filter expectedFilter = FilterBuilder.create().onName("Test filter 2").build();
    Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
    assertEquals(expectedFilter, actualFilter);
  }

  @Test
  public void testWildcardFilterSerialization() {
    String stringToTest = "name:Test filter *";
    Filter expectedFilter = FilterBuilder.create().onName("Test filter *").build();
    Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
    assertEquals(expectedFilter, actualFilter);
  }

  @Test
  public void testEscapeSerialization() {
    String stringToTest = "attr:(?\\:O)organism";
    Filter expectedFilter = FilterBuilder.create().onAttribute("(?:O)organism").build();
    Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
    assertEquals(expectedFilter, actualFilter);
  }

  @Test
  public void testForDifferentFiltersWithEscapedCharacters() {
    String stringToTest = "attr:(?\\:O)rganism";
    Filter wrongDeserializedFilter =
        FilterBuilder.create().onAttribute("(?").withValue("O)rganism").build();
    Filter actualFilter = FilterBuilder.create().buildFromString(stringToTest);
    assertNotEquals(wrongDeserializedFilter, actualFilter);
  }
}
