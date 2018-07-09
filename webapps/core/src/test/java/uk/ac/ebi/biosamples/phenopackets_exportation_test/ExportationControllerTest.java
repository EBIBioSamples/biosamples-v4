package uk.ac.ebi.biosamples.phenopackets_exportation_test;

import static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import uk.ac.ebi.biosamples.Application;
import uk.ac.ebi.biosamples.controller.SampleRestController;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,classes = Application.class )
@TestPropertySource(
        locations = "classpath:test.properties")
@AutoConfigureMockMvc

public class ExportationControllerTest {

    @Autowired
    private SampleRestController controller;

    @Autowired
    MockMvc mvc;

    @Test
    public void contexLoads() throws Exception {
        assertThat(controller).isNotNull();
    }

//    @Test public void checkCallingnonExistanseSample() throws Exception {
//        mvc.perform(get("samples/nonexistant.json?type=phenopacket").contentType(MediaType.APPLICATION_JSON_UTF8))
//                .andExpect(status().isNotFound());
//    }
    @Test public void checkCallingExistanseSample() throws Exception {
        mvc.perform(get("samples/SAMEA910970.json?type=phenopacket").contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$biosamples.id").isString());
    }
}