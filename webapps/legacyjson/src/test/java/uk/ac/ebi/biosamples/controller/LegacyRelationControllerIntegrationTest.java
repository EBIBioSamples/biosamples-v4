package uk.ac.ebi.biosamples.controller;

import org.junit.runner.RunWith;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.ac.ebi.biosamples.TestSample;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.SampleService;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
public class LegacyRelationControllerIntegrationTest {

    @MockBean
    private SampleService sampleService;

    @Autowired
    private MockMvc mockMvc;

    private ResultActions getHAL(String template, Object... uriParameters) throws Exception {
        return mockMvc.perform(get(template, uriParameters).accept(MediaTypes.HAL_JSON_VALUE));
    }

    private ResultActions getRelationsHAL(String accession) throws Exception {
        return mockMvc.perform(get("/samplesrelations/{accession}", accession).accept(MediaTypes.HAL_JSON_VALUE));
    }

    @Test
    public void testReturnRelationByAccession() throws Exception {
        Sample testSample = new TestSample("RELATION").build();
        when(sampleService.findByAccession(anyString())).thenReturn(testSample);

        getRelationsHAL("anyAccession")
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/hal+json;charset=UTF-8"))
                .andExpect(jsonPath("$.accession").value(testSample.getAccession()));
    }

    @Test
    public void testRelationsHasSelfLink() throws Exception {
        Sample testSample = new TestSample("RELATION").build();
        when(sampleService.findByAccession(anyString())).thenReturn(testSample);

        getRelationsHAL("anyAccession")
                .andExpect(jsonPath("$._links.self").exists());
    }

}
