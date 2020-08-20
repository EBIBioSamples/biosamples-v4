package uk.ac.ebi.biosamples.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Recommendation;
import uk.ac.ebi.biosamples.model.Sample;

import java.time.Instant;
import java.util.SortedSet;
import java.util.TreeSet;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class RecommendationServiceTest {

    @Autowired
    RecommendationService recommendationService;

    @Test
    public void should_return_recommendation() {
        Sample sample = getTestSample();
        Recommendation recommendation = recommendationService.getRecommendations(sample);

        Assert.assertEquals(1, recommendation.getGoodAttributes().size());
        Assert.assertEquals(1, recommendation.getBadAttributes().size());
        Assert.assertEquals(40, recommendation.getQuality());
    }

    private Sample getTestSample() {
        String name = "RecommendationServiceUnitTest_sample";
        Instant release = Instant.parse("2016-04-01T11:36:57.00Z");
        SortedSet<Attribute> attributes = new TreeSet<>();
        attributes.add(Attribute.build("organism", "Homo sapiens"));
        attributes.add(Attribute.build("organism_part", "liver"));

        return new Sample.Builder(name)
                .withDomain("self.biosamplesUnitTests")
                .withRelease(release)
                .withAttributes(attributes)
                .build();
    }

}
