package uk.ac.ebi.biosamples.curation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biosamples.BioSamplesProperties;
import uk.ac.ebi.biosamples.client.BioSamplesClient;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.ols.OlsProcessor;
import uk.ac.ebi.biosamples.service.CurationApplicationService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureWebClient
public class SampleCurationCallableTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OlsProcessor olsProcessor;

    @Autowired
    private BioSamplesProperties bioSamplesProperties;

    private MockRestServiceServer mockServer;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MockBioSamplesClient mockBioSamplesClient;

    @Autowired
    private CurationApplicationService curationApplicationService;

    @Before
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void given_known_incorrect_sample_ensure_curated_correctly() throws Exception {
        String filePath = "/examples/samples/SAMEA103887543.json";
        Sample sample = objectMapper.readValue(SampleCurationCallableTest.class.getResourceAsStream(filePath), Sample.class);
        String shortcode = "PATO_0000384";
        String expectedResponse = readFile("/examples/ols-responses/" + shortcode + ".json");
        mockServer.expect(requestTo("https://wwwdev.ebi.ac.uk/ols/api/terms?id=" + shortcode + "&size=500")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(expectedResponse, MediaType.APPLICATION_JSON));
        SampleCurationCallable sampleCurationCallable = new SampleCurationCallable(mockBioSamplesClient, sample, olsProcessor, curationApplicationService, null);
        sampleCurationCallable.call();
        Sample curatedSample = curationApplicationService.applyAllCurationToSample(sample, mockBioSamplesClient.getCurations(sample.getAccession()));
        String curatedFilePath = "/examples/samples/SAMEA103887543-curated.json";
        Sample expectedCuratedSample = objectMapper.readValue(SampleCurationCallableTest.class.getResourceAsStream(curatedFilePath), Sample.class);
        assertEquals(expectedCuratedSample, curatedSample);
    }

    private String readFile(String filePath) throws IOException {
        InputStream inputStream = SampleCurationCallableTest.class.getResourceAsStream(filePath);
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
