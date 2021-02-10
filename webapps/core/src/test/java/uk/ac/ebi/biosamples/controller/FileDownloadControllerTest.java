package uk.ac.ebi.biosamples.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"aap.domains.url = ''"})
@AutoConfigureMockMvc
public class FileDownloadControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FileDownloadController fileDownloadController;

    @Test
    public void download() throws Exception {
        mockMvc.perform(get("/download").contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isOk());
    }
}