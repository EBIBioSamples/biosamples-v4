package uk.ac.ebi.biosamples.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Sample;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class SampleReadServiceTest {

    @Autowired
    private SampleService sampleService;

    @Test
    public void testStore() {
        Sample sample = generateTestSample("test", Collections.EMPTY_LIST);
        Sample savedSample = sampleService.store(sample);
        assertNotNull(savedSample);
    }

    private Sample generateTestSample(String accession, List<Attribute> attributes) {
        Set<Attribute> attributeSet = new HashSet<>();
        for (Attribute attribute : attributes) {
            attributeSet.add(attribute);
        }
        return Sample.build("", accession, "", Instant.now(), Instant.now(), attributeSet, Collections.emptySet(), Collections.emptySet());
    }
}
