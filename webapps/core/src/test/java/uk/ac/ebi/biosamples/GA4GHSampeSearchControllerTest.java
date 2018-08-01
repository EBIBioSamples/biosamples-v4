package uk.ac.ebi.biosamples;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.biosamples.controller.GA4GHSampeSearchController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = Application.class)
@TestPropertySource(
        locations = "classpath:test.properties")
@AutoConfigureMockMvc
public class GA4GHSampeSearchControllerTest {

    @Autowired
    private GA4GHSampeSearchController controller;
    @Autowired
    MockMvc mvc;

    @Autowired
    MockMvc mvc1;

    @Test
    public void contexLoads() {
        assertThat(controller).isNotNull();
    }

    @Test
    public void checkCallingEmptyDataset() throws Exception {
        String response = mvc.perform(get("/samples/ga4gh?disease=&page=1").contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JSONAssert.assertEquals(response, "{}", false);
    }
}