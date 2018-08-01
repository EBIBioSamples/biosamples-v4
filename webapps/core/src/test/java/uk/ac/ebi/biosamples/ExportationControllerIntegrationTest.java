package uk.ac.ebi.biosamples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.biosamples.controller.SampleRestController;


/**
 * Integraion testing of phenopackets exportatioon and testing of controller. You need to have access to
 * https://www.ebi.ac.uk/ols/api for test performing. Also you should get token from https://explore.aap.tsi.ebi.ac.uk/auth and
 * write it in token attribute. For more detailed description check https://github.com/EBIBioSamples/biosamples-v4/tree/g-summer-code.
 */

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = Application.class)
@TestPropertySource(
        locations = "classpath:test.properties")
@AutoConfigureMockMvc

public class ExportationControllerIntegrationTest {

    final String path = "/sample.json";
    final String phenopacketPath = "/phenopacket.json";

    @Autowired
    private SampleRestController controller;
    @Autowired
    MockMvc mvc;

    @Autowired
    MockMvc mvc1;

    @Test
    public void contexLoads() {
        assertThat(controller).isNotNull();
    }

    @Test
    public void checkCallingnonExistanseSample() throws Exception {
        mvc.perform(get("samples/nonexistant.json?type=phenopacket").contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());
    }

}