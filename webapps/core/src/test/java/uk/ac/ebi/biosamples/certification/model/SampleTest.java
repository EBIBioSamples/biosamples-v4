package uk.ac.ebi.biosamples.certification.model;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import uk.ac.ebi.biosamples.model.certification.SampleDocument;

import static org.junit.Assert.assertEquals;

public class SampleTest {
    @Test
    public void ensure_sample_has_a_hash() throws Exception {
        String data = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("json/ncbi-SAMN03894263-curated.json"), "UTF8");
        SampleDocument sampleDocument = new SampleDocument("test-uuid", data);
        assertEquals(32, sampleDocument.getHash().length());
    }
}
