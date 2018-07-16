package uk.ac.ebi.biosamples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.junit.Test;
import org.junit.runner.RunWith;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.biosamples.controller.SampleRestController;

import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.skyscreamer.jsonassert.Customization;

import java.io.File;

/**
 * Integraion testing of phenopackets exportatioon and testing of controller. You need to have access to
 * https://www.ebi.ac.uk/ols/api for test performing. Also you should get token from https://explore.aap.tsi.ebi.ac.uk/auth and
 * write it in token attribute. For more detailed description check https://github.com/EBIBioSamples/biosamples-v4/tree/g-summer-code.
 *
 */

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = Application.class)
@TestPropertySource(
        locations = "classpath:test.properties")
@AutoConfigureMockMvc

public class ExportationControllerIntegrationTest {

    final String path = "/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/core/src/test/java/uk/ac/ebi/biosamples/model/phenopackets_test_cases/sample.json";
    final String phenopacketPath = "/Users/dilsatsalihov/Desktop/gsoc/biosamples-v4/models/core/src/test/java/uk/ac/ebi/biosamples/model/phenopackets_test_cases/phenopacket.json";
    final String token = "Bearer $TOKEN"; //TODO update token

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

    @Test public void checkCallingnonExistanseSample() throws Exception {
        mvc.perform(get("samples/nonexistant.json?type=phenopacket").contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());
    }

    @Test
    public void checkCallingExistanseSample() throws Exception {
        String sample = Files.toString(new File(path), Charsets.UTF_8);
        String phenopacket = Files.toString(new File(phenopacketPath), Charsets.UTF_8);
        String actualJson = "";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(1568);
        headers
                .add("Authorization", token);
        mvc1.perform(post("/samples").contentType(MediaType.APPLICATION_JSON_VALUE).content(sample).headers(headers)).andExpect(status().isCreated());
        actualJson = mvc1.perform(get("/samples/SAMEA100000.phenopacket").contentType("appication/phenopacket+json"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        org.skyscreamer.jsonassert.JSONAssert.assertEquals(actualJson, phenopacket, new CustomComparator(JSONCompareMode.LENIENT,
                new Customization("metaData.created", (o1, o2) -> true)));
    }
}