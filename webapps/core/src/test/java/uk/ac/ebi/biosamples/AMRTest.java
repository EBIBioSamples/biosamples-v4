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
import uk.ac.ebi.biosamples.service.SampleService;

import java.time.Instant;
import java.util.Optional;
import java.util.TreeSet;

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

    @Test
    public void givenSample_whenGetSample_thenReturnJsonObject() throws Exception {
        Sample sample = Sample.build(
                "testSample", "TEST1", "foozit", Instant.now(), Instant.now(),
                new TreeSet<>(), new TreeSet<>(), new TreeSet<>(), new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
        when(readService.fetch(eq("TEST1"), any())).thenReturn(Optional.of(sample));


        mockMvc.perform(get("/samples/TEST1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(sample.getName())))
                .andExpect(jsonPath("$.accession", is(sample.getAccession())));

    }

    @Test
    public void givenSampleWithStructuredData_whenGetSample_thenReturnStructuredDataInJson() throws Exception {
    }

}
