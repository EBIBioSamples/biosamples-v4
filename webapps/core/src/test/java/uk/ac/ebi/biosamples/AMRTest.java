package uk.ac.ebi.biosamples;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.AMREntry;
import uk.ac.ebi.biosamples.model.structured.AMRTable;
import uk.ac.ebi.biosamples.service.SampleService;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AMRTest {

    /*
    @Autowired
    private SampleRestController sampleRestController;

    @Test
    public void contextLoads() throws Exception {
        assertThat(sampleRestController).isNotNull();
    }
    */
    @Autowired
    private MockMvc mockMvc;

    private JacksonTester<Sample> json;

    @MockBean
    private SampleService readService;

    private AMREntry getAMREntry() {
        return new AMREntry.Builder()
                .withAntibiotic("ampicillin")
                .withResistancePhenotype("susceptible")
                .withMeasure("==", 10, "mg/L")
                .withVendor("in-house")
                .withLaboratoryTypingMethod("CMAD")
                .withTestingStandard("CLD")
                .build();
    }

    private AMRTable getAMRTable() {
        return new AMRTable.Builder("http://schema.org")
                .withEntry(getAMREntry())
                .build();
    }

    private Sample.Builder getTestSampleBuilder() {
        return new Sample.Builder("testSample", "TEST1")
                .withDomain("foozit").withReleaseDate(Instant.now()).withUpdateDate(Instant.now());
    }

    @Test
    public void givenSample_whenGetSample_thenReturnJsonObject() throws Exception {
        Sample sample = getTestSampleBuilder().build();

        when(readService.fetch(eq(sample.getAccession()), any())).thenReturn(Optional.of(sample));


        mockMvc.perform(get("/samples/{accession}", sample.getAccession())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(sample.getName())))
                .andExpect(jsonPath("$.accession", is(sample.getAccession())));

    }

    @Test
    public void givenSampleWithStructuredData_whenGetSample_thenReturnStructuredDataInJson() throws Exception {
        AMREntry amrEntry = getAMREntry();
        AMRTable amrTable = new AMRTable.Builder("http://schema.org")
                .withEntry(amrEntry).build();


        Sample sample = getTestSampleBuilder().addData(amrTable).build();
        when(readService.fetch(eq(sample.getAccession()), any())).thenReturn(Optional.of(sample));

        mockMvc.perform(get("/samples/{accession}", sample.getAccession()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").value(allOf(
                        hasEntry("schema", "http://schema.org"),
                        hasEntry("type", "AMR"),
                        hasKey("content"))))
                .andExpect(jsonPath("$.data[0].content").isArray())
                .andExpect(jsonPath("$.data[0].content[0]").value(allOf(
                        hasEntry("antibiotic", amrEntry.getAntibiotic()),
                        hasEntry("resistancePhenotype", amrEntry.getResistancePhenotype()),
                        hasEntry("measurementSign", amrEntry.getMeasurementSign()),
                        hasEntry("measurementUnit", amrEntry.getMeasurementUnit()),
                        hasEntry("vendor", amrEntry.getVendor()),
                        hasEntry("laboratoryTypingMethod", amrEntry.getLaboratoryTypingMethod()),
                        hasEntry("testingStandard", amrEntry.getTestingStandard())
                        )))
                .andExpect(jsonPath("$.data[0].content[0]").value(
                        hasEntry("measurementValue", amrEntry.getMeasurementValue()) // This needs to go here because the the hasEntry has a different signature - Only one having a number as a value. allOf wants all matchers of the same type
                ));

    }

    @Test
    public void able_to_submit_sample_with_structuredData() throws Exception {

//        URL amrJsonUrl = this.getClass().getResource("/amr_sample.json");

        String json = StreamUtils.copyToString(new ClassPathResource("amr_sample.json").getInputStream(), Charset.defaultCharset());

        mockMvc.perform(post("/samples")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(json)
            ).andExpect(status().isOk());

    }

}
