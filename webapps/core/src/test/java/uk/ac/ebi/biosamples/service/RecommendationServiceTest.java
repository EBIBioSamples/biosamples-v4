/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.service;

import java.time.Instant;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.models.curami.AttributeRecommendation;
import uk.ac.ebi.biosamples.models.curami.CuramiRecommendation;
import uk.ac.ebi.biosamples.model.Sample;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class RecommendationServiceTest {

  @Autowired RecommendationService recommendationService;

  @Test
  public void should_return_recommendation() {
    Sample sample = getTestSample();
    CuramiRecommendation recommendation = recommendationService.getRecommendations(sample);

    Assert.assertEquals(1, recommendation.getKnownAttributes().size());
    Assert.assertEquals(1, recommendation.getAttributeRecommendations().size());
    Assert.assertEquals(15, recommendation.getQuality());
  }

  @Test
  public void should_find_recommendations_from_curations() {
    Sample sample = getTestSample2();
    CuramiRecommendation recommendation = recommendationService.getRecommendations(sample);

    Assert.assertTrue(
        recommendation
            .getAttributeRecommendations()
            .contains(
                new AttributeRecommendation.Builder()
                    .withAttribute("Gender")
                    .withRecommendation("sex")
                    .build()));
  }

  @Test
  public void should_exist_unidentified_attribute() {
    Sample sample = getTestSample2();
    CuramiRecommendation recommendation = recommendationService.getRecommendations(sample);

    Assert.assertEquals(1, recommendation.getUnknownAttributes().size());
  }

  @Test
  public void should_return_recommended_sample() {
    Sample sample = getTestSample2();
    CuramiRecommendation recommendation = recommendationService.getRecommendations(sample);
    Sample recommendedSample = recommendationService.getRecommendedSample(sample, recommendation);

    Optional<Attribute> recAttr =
        recommendedSample.getAttributes().stream()
            .filter(a -> a.getType().equals("sex"))
            .findFirst();
    Assert.assertTrue(recAttr.isPresent());
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

  private Sample getTestSample2() {
    String name = "RecommendationServiceUnitTest_sample_2";
    Instant release = Instant.parse("2016-04-01T11:36:57.00Z");
    SortedSet<Attribute> attributes = new TreeSet<>();
    attributes.add(Attribute.build("organism", "Homo sapiens"));
    attributes.add(Attribute.build("organism_part", "liver"));
    attributes.add(Attribute.build("INSDC_status", "x"));
    attributes.add(Attribute.build("STUDY NAME", "x"));
    attributes.add(Attribute.build("gap accession", "x"));
    attributes.add(Attribute.build("Age", "56"));
    attributes.add(Attribute.build("Phenotype", "x"));
    attributes.add(Attribute.build("Disease", "x"));
    attributes.add(Attribute.build("Gender", "x"));
    attributes.add(Attribute.build("impossibleAttribute", "x"));

    return new Sample.Builder(name)
        .withDomain("self.biosamplesUnitTests")
        .withRelease(release)
        .withAttributes(attributes)
        .build();
  }
}
