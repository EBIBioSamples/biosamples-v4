package uk.ac.ebi.biosamples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.model.structured.amr.AMREntry;
import uk.ac.ebi.biosamples.model.structured.amr.AMRTable;
import uk.ac.ebi.biosamples.model.structured.amr.AmrPair;
import uk.ac.ebi.biosamples.service.BioSamplesAapService;
import uk.ac.ebi.biosamples.service.SampleService;
import uk.ac.ebi.biosamples.service.SchemaValidatorService;

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
public class StructuredDataTest {

    @Autowired
    private MockMvc mockMvc;

    private JacksonTester<Sample> json;

    private ObjectMapper mapper;

    @MockBean
    private BioSamplesAapService bioSamplesAapService;

    @MockBean
    private SampleService sampleService;

    @MockBean
    private SchemaValidatorService schemaValidatorService;

    @Before
    public void init() {
        mapper = new ObjectMapper();
    }

    @Test
    public void able_to_submit_sample_with_structuredData() throws Exception {
        String json = StreamUtils.copyToString(new ClassPathResource("structured_data_sample.json").getInputStream(), Charset.defaultCharset());
        Sample sample = mapper.readValue(json, Sample.class);
        Assert.assertEquals(3, sample.getData().size());
    }
}
