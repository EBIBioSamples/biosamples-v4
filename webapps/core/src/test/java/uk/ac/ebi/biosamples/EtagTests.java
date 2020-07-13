package uk.ac.ebi.biosamples;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.SampleService;

import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EtagTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BioSamplesAapService bioSamplesAapService;

    @MockBean
    private SampleService sampleService;

    @Test
    public void get_validation_endpoint_return_not_allowed_response() throws Exception {
        String sampleAccession = "SAMEA123456789";
        Sample testSample = new Sample.Builder("TestSample", sampleAccession)
                .withDomain("TestDomain")
                .addAttribute(new Attribute.Builder("Organism", "Homo sapiens").build())
                .build();

        when(sampleService.fetch(Matchers.eq(sampleAccession), Matchers.any(Optional.class), any(String.class))).thenReturn(Optional.of(testSample));
        when(bioSamplesAapService.handleSampleDomain(testSample)).thenReturn(testSample);

        MvcResult sampleRequestResult = mockMvc.perform(get("/samples/{accession}", sampleAccession).accept(MediaType.APPLICATION_JSON))
                .andReturn();

        String etag = sampleRequestResult.getResponse().getHeader("Etag");

        mockMvc.perform(get("/samples/{accession}", sampleAccession).accept(MediaType.APPLICATION_JSON)
                .header("If-None-Match", etag))
                .andExpect(status().isNotModified());


    }
}
