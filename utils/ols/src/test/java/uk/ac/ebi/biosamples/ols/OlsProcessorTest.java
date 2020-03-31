package uk.ac.ebi.biosamples.ols;

import com.google.common.collect.ImmutableMap;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureWebClient
public class OlsProcessorTest {

    private MockRestServiceServer mockServer;

    @Autowired
    private OlsProcessor olsProcessor;

    @Autowired
    private RestTemplate restTemplate;

    @Before
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    private Map<String, String> expectedValues = ImmutableMap.of(
            "NCIT:C2985", "http://purl.obolibrary.org/obo/NCIT_C2985",
            "UBERON:0003978", "http://purl.obolibrary.org/obo/UBERON_0003978",
            "FOODON:03304708", "http://purl.obolibrary.org/obo/FOODON_03304708",
            "NCIT_C14207", "http://purl.obolibrary.org/obo/NCIT_C14207"
    );

    @Test
    public void test_OlsProcessor_returns_correct_value_for_invalid_ols_term() throws IOException {
        String shortcode = "invalid-term";
        Optional<String> result = performQuery(shortcode);
        assertFalse(result.isPresent());
    }

    private String readFile(String filePath) throws IOException {
        InputStream inputStream = OlsProcessorTest.class.getResourceAsStream(filePath);
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    private Optional<String> performQuery(String shortcode) throws IOException {
        String expectedResponse = readFile("/examples/ols-responses/" + shortcode + ".json");
        mockServer.reset();
        mockServer.expect(requestTo("https://www.ebi.ac.uk/ols/api/terms?id=" + shortcode + "&size=500")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(expectedResponse, MediaType.APPLICATION_JSON));
        Optional<String> result = olsProcessor.queryOlsForShortcode(shortcode);
        mockServer.verify();
        return result;
    }

    @Test
    public void test_OlsProcessor_returns_correct_value_for_multi_ols_term() throws IOException {
        String shortcode = "PATO_0000384";
        Optional<String> result = performQuery(shortcode);
        assertTrue(result.isPresent());
        assertEquals("http://purl.obolibrary.org/obo/PATO_0000384", result.get());
    }

    @Test
    public void test_OlsProcessor_returns_correct_value_for_standard_ols_term_that_has_defining_ontology() throws IOException {
        String shortcode = "NCBITaxon_3702";
        Optional<String> result = performQuery(shortcode);
        assertTrue(result.isPresent());
        assertEquals("http://purl.obolibrary.org/obo/NCBITaxon_3702", result.get());
    }

    @Test
    public void test_OlsProcessor_returns_correct_value_for_standard_ols_term_does_not_have_defining_ontology() throws IOException {
        String shortcode = "FBcv_0003016";
        Optional<String> result = performQuery(shortcode);
        assertTrue(result.isPresent());
        assertEquals("http://purl.obolibrary.org/obo/FBcv_0003016", result.get());
    }
    /*@Test
    public void test_OlsProcessor_returns_correct_value_for_example_terms() throws IOException {
        for (Map.Entry<String, String> entry : expectedValues.entrySet()) {
            Optional<String> result = performQuery(entry.getKey());
            assertTrue(result.isPresent());
            assertEquals(entry.getValue(), result.get());
        }
    }*/
}
