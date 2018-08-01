package uk.ac.ebi.biosamples.ols;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureWebClient
public class OlsProcessorTest {

    @Autowired
    private OlsProcessor olsProcessor;

    private Map<String, String> expectedValues = ImmutableMap.of(
            "NCIT:C2985", "http://purl.obolibrary.org/obo/NCIT_C2985",
            "ENVO:00000294", "http://purl.obolibrary.org/obo/ENVO_00000294",
            "UBERON:0003978", "http://purl.obolibrary.org/obo/UBERON_0003978",
            "FOODON:03304708", "http://purl.obolibrary.org/obo/FOODON_03304708",
            "NCIT_C14207", "http://purl.obolibrary.org/obo/NCIT_C14207"
    );

    @Test
    public void test_OlsProcessor_returns_correct_value_for_invalid_ols_term() {
        Optional<String> result = olsProcessor.queryOlsForShortcode("invalid-term");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_OlsProcessor_returns_correct_value_for_multi_ols_term() {
        Optional<String> result = olsProcessor.queryOlsForShortcode("PATO_0000384");
        assertTrue(result.isPresent());
        assertEquals("http://purl.obolibrary.org/obo/PATO_0000384", result.get());
    }

    @Test
    public void test_OlsProcessor_returns_correct_value_for_standard_ols_term_that_has_defining_ontology() {
        Optional<String> result = olsProcessor.queryOlsForShortcode("NCBITaxon_3702");
        assertTrue(result.isPresent());
        assertEquals("http://purl.obolibrary.org/obo/NCBITaxon_3702", result.get());
    }

    @Test
    public void test_OlsProcessor_returns_correct_value_for_standard_ols_term_does_not_have_defining_ontology() {
        Optional<String> result = olsProcessor.queryOlsForShortcode("FBcv_0003016");
        assertTrue(result.isPresent());
        assertEquals("http://purl.obolibrary.org/obo/FBcv_0003016", result.get());
    }

    @Test
    public void test_OlsProcessor_returns_correct_value_for_example_terms() {
        for (Map.Entry<String, String> entry : expectedValues.entrySet()) {
            Optional<String> result = olsProcessor.queryOlsForShortcode(entry.getKey());
            assertTrue(result.isPresent());
            assertEquals(entry.getValue(), result.get());
        }

    }
}
