package uk.ac.ebi.biosamples.legacy.json.service;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes={JSONSampleToSampleConverter.class})
public class LegacyJSONDeserializationTest {

    private Logger log = LoggerFactory.getLogger(getClass());
    String sampleJson;
    String groupJson;
    Sample legacySample;
    Sample legacyGroup;

    @Autowired
    JSONSampleToSampleConverter sampleConverter;

    @Before
    public void setup() throws IOException {
        sampleJson = Resources.toString(Resources.getResource("testLegacySample.json"), Charsets.UTF_8);
//        groupJson = Resources.toString(Resources.getResource("testLegacyGroup.json"), Charsets.UTF_8);

        legacySample = sampleConverter.convert(sampleJson);
//        legacyGroup = sampleConverter.convert(groupJson);
    }

    @Test
    public void testConvertAccession() {
        assertThat(legacySample.getAccession()).isEqualTo("SAMEA104493031");
    }

    @Test
    public void testConvertDates() {
        LocalDate updateDate = legacySample.getUpdate().atZone(ZoneId.of("UTC")).toLocalDate();
        assertThat(updateDate.getYear()).isEqualTo(2017);
        assertThat(updateDate.getMonth()).isEqualTo(Month.NOVEMBER);
        assertThat(updateDate.getDayOfMonth()).isEqualTo(19);

        LocalDate releaseDate = legacySample.getRelease().atZone(ZoneId.of("UTC")).toLocalDate();
        assertThat(releaseDate.getYear()).isEqualTo(2017);
        assertThat(releaseDate.getMonth()).isEqualTo(Month.AUGUST);
        assertThat(releaseDate.getDayOfMonth()).isEqualTo(30);
    }

    @Test
    public void testConvertDescriptionToAttribute() {
        assertThat(legacySample.getAttributes()).contains(Attribute.build("description", "This is a simple description"));
    }

    @Test
    public void testHasRightNumberOfAttributes() {
        assertThat(legacySample.getAttributes().size()).isEqualTo(4);
    }

    @Test
    public void testContainsCertainAttributes() {
        assertThat(legacySample.getAttributes().stream().map(Attribute::getType).collect(Collectors.toList()))
                .containsAll(Arrays.asList("organism", "diseaseState", "cellLine"));
    }

    @Test
    public void testAttributeContainsOntologyTerms() {
        List<Attribute> organismAttribute = legacySample.getAttributes().stream().filter(att->att.getType().equals("organism")).collect(Collectors.toList());
        assertThat(organismAttribute).hasSize(1);
        assertThat(organismAttribute.get(0).getIri()).containsOnlyOnce("http://purl.obolibrary.org/obo/NCBITaxon_9606");

    }
}
