package uk.ac.ebi.biosamples.solr;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class MessageHandlerSolrTest {

    @Test
    public void should_index_public_sample() throws Exception {
        Attribute attribute = Attribute.build("INSDC status", "public");
        assertTrue(MessageHandlerSolr.isIndexingCandidate(generateTestSample("public-example", Collections.singletonList(attribute))));
    }

    @Test
    public void should_index_live_sample() throws Exception {
        Attribute attribute = Attribute.build("INSDC status", "live");
        assertTrue(MessageHandlerSolr.isIndexingCandidate((generateTestSample("live-example", Collections.singletonList(attribute)))));
    }

    @Test
    public void should_index_sample_with_no_INSDC_status() throws Exception {
        assertTrue(MessageHandlerSolr.isIndexingCandidate((generateTestSample("no-example", Collections.EMPTY_LIST))));
    }

    @Test
    public void should_not_index_suppressed_sample() throws Exception {
        Attribute attribute = Attribute.build("INSDC status", "suppressed");
        assertFalse(MessageHandlerSolr.isIndexingCandidate((generateTestSample("suppressed-example", Collections.singletonList(attribute)))));
    }

    @Test
    public void should_not_index_sample_with_unexpected_INSDC_status() throws Exception {
        Attribute attribute = Attribute.build("INSDC status", "gertgerge");
        assertFalse(MessageHandlerSolr.isIndexingCandidate((generateTestSample("unexpected-example", Collections.singletonList(attribute)))));
    }

    private Sample generateTestSample(String accession, List<Attribute> attributes) {
        Set<Attribute> attributeSet = new HashSet<>();
        for (Attribute attribute : attributes) {
            attributeSet.add(attribute);
        }
        return Sample.build("", accession, "", Instant.now(), Instant.now(), attributeSet, Collections.emptySet(), Collections.emptySet());
    }
}
