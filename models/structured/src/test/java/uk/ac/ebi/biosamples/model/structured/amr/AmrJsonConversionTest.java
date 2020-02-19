package uk.ac.ebi.biosamples.model.structured.amr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Java6Assertions.assertThat;

@JsonTest
@TestPropertySource(properties={"spring.jackson.serialization.INDENT_OUTPUT=true"})
public class AmrJsonConversionTest {

    Logger log = LoggerFactory.getLogger(getClass());

    JacksonTester<AMREntry> amrEntryJacksonTester;
    JacksonTester<AMRTable> amrTableJacksonTester;

    @Before
    public void setup() {
        ObjectMapper objectMapper = new ObjectMapper();
        JacksonTester.initFields(this, objectMapper);
    }

    @Test
    public void testAmrEntrySerializer() throws IOException {
        AMREntry entry = new AMREntry.Builder()
                .withAntibiotic(new AmrPair("A",""))
                .withResistancePhenotype("Something")
                .withMeasure("==", "2", "mg/L")
                .withVendor("in-house")
                .withAstStandard("VeryHighStandard")
                .withLaboratoryTypingMethod("TypingMethod")
                .build();

        JsonContent<AMREntry> json = this.amrEntryJacksonTester.write(entry);

        log.info(json.getJson());

        assertThat(json).hasJsonPathStringValue("@.antibiotic.value", "A");
    }

    @Test
    public void testAmrTableSerializer() throws IOException {
        AMRTable.Builder tableBuilder = new AMRTable.Builder("http://some-fake-schema.com");
        tableBuilder.addEntry(new AMREntry.Builder()
                .withAntibiotic(new AmrPair("A"))
                .withResistancePhenotype("Something")
                .withMeasure("==", "14", "mg/L")
                .withVendor("in-house")
                .withAstStandard("VeryHighStandard")
                .withLaboratoryTypingMethod("TypingMethod")
                .build());

        tableBuilder.addEntry(new AMREntry.Builder()
                .withAntibiotic(new AmrPair("B",""))
                .withResistancePhenotype("pectine")
                .withMeasure(">=", "14", "mg/L")
                .withVendor("GSKey")
                .withAstStandard("low-quality-standard")
                .withLaboratoryTypingMethod("Nothing")
                .build());

        AMRTable table = tableBuilder.build();

        JsonContent<AMRTable> json = this.amrTableJacksonTester.write(table);
        log.info(json.getJson());

        assertThat(json).hasJsonPathValue("@.schema", "http://some-fake-schema.com");
        assertThat(json).hasJsonPathValue("@.type", "AMR");

        assertThat(json).hasJsonPathArrayValue("@.content");
        assertThat(json).extractingJsonPathArrayValue("@.content").hasSize(2);
        assertThat(json).extractingJsonPathArrayValue("@.content").hasOnlyElementsOfType(LinkedHashMap.class);

        assertThat(json).extractingJsonPathMapValue("@.content[1]").containsKeys(
                "antibiotic", "resistance_phenotype", "ast_standard", "vendor", "measurement_units",
                "laboratory_typing_method", "measurement_sign", "measurement"
        );
        assertThat(json).extractingJsonPathMapValue("@.content[1]").containsEntry("measurement", "14");

    }

    @Test
    public void testAMRDeserialization() throws Exception{
        AMRTable.Builder tableBuilder = new AMRTable.Builder("test");
        tableBuilder.addEntry(new AMREntry.Builder()
                .withAntibiotic(new AmrPair("ampicillin","test.org"))
                .withResistancePhenotype("susceptible")
                .withMeasure("==", "2", "mg/L")
                .withVendor("in-house")
                .withAstStandard("CLSI")
                .withLaboratoryTypingMethod("MIC")
                .build());
        AMRTable table = tableBuilder.build();
        // Assert sample with AMR table entry

        assertThat(this.amrTableJacksonTester.readObject("/AmrData.json")).isEqualTo(table);


    }

    @Test
    public void testDeserializationEnaAMR() throws Exception {
        AMREntry entry = new AMREntry.Builder()
                .withAntibiotic(new AmrPair("Ampicillin","test.org"))
                .withAstStandard("EUCAST")
                .withBreakpointVersion("not_determined")
                .withLaboratoryTypingMethod("Agar dilution")
                .withMeasure("=", "8", "mg/L")
                .withPlatform("-")
                .withResistancePhenotype("not-defined")
                .withSpecies(new AmrPair("Escherichia coli"))
                .build();

        assertThat(this.amrEntryJacksonTester.readObject("/EnaAmrData.json")).isEqualTo(entry);
    }


}
