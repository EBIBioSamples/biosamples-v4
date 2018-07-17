package uk.ac.ebi.biosamples;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.AMREntry;
import uk.ac.ebi.biosamples.model.structured.AMRTable;
import uk.ac.ebi.biosamples.service.SampleService;

import java.time.Instant;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        AMRTable amrTable = new AMRTable.Builder("http://schema.org")
                .withEntry(getAMREntry()).build();


        Sample sample = getTestSampleBuilder().addData(amrTable) .build();

    }

}
