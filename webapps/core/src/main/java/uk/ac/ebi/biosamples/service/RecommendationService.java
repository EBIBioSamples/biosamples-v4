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

import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.AttributeRecommendation;
import uk.ac.ebi.biosamples.model.CuramiRecommendation;
import uk.ac.ebi.biosamples.model.Sample;

@Service
public class RecommendationService {
  private final DataLoader dataLoader;

  public RecommendationService() {
    dataLoader = new DataLoader();
  }

  @PostConstruct
  private void init() {
    dataLoader.loadDataFromClassPathResource();
  }

  public CuramiRecommendation getRecommendations(Sample sample) {
    SortedSet<String> attributes =
        new TreeSet<>(
            sample.getAttributes().stream().map(Attribute::getType).collect(Collectors.toList()));

    SortedSet<String> goodAttributes = new TreeSet<>();
    SortedSet<AttributeRecommendation> badAttributes = new TreeSet<>();
    SortedSet<String> missingAttributes = new TreeSet<>();

    for (String attribute : attributes) {
      if (dataLoader.getPopularAttributes().contains(attribute)) {
        goodAttributes.add(attribute);
        continue;
      } else if (dataLoader.getCurations().containsKey(attribute)) {
        badAttributes.add(
            new AttributeRecommendation.Builder()
                .withAttribute(attribute)
                .withRecommendation(dataLoader.getCurations().get(attribute))
                .build());
        continue;
      }

      String normalisedAttribute =
          CuramiUtils.normaliseAttribute(attribute, dataLoader.getAbbreviations());
      if (dataLoader.getCurations().containsKey(normalisedAttribute)) {
        badAttributes.add(
            new AttributeRecommendation.Builder()
                .withAttribute(attribute)
                .withRecommendation(dataLoader.getCurations().get(normalisedAttribute))
                .build());
        continue;
      } else if (dataLoader.getPopularAttributes().contains(normalisedAttribute)) {
        badAttributes.add(
            new AttributeRecommendation.Builder()
                .withAttribute(attribute)
                .withRecommendation(normalisedAttribute)
                .build());
        continue;
      }

      Optional<String> similarAttribute =
          CuramiUtils.getMostSimilarAttribute(attribute, dataLoader.getPopularAttributes());
      if (similarAttribute.isPresent()) {
        badAttributes.add(
            new AttributeRecommendation.Builder()
                .withAttribute(attribute)
                .withRecommendation(similarAttribute.get())
                .build());
      } else {
        missingAttributes.add(attribute);
      }
    }

    // have at least 5 known attributes to score 50 (4 similar attributes to score 20)
    int attributeQuality =
        Math.min(50 * goodAttributes.size() / 5 + Math.min(5 * badAttributes.size(), 20), 50);
    int quality = getSampleQualityScore(sample, attributeQuality);

    return new CuramiRecommendation.Builder()
        .withQuality(quality)
        .withGoodAttributes(goodAttributes)
        .withBadAttributes(badAttributes)
        .withMissingAttributes(missingAttributes)
        .build();
  }

  public Sample getRecommendedSample(Sample sample, CuramiRecommendation recommendation) {
    Sample recommendedSample = sample;
    if (!recommendation.getAttributeRecommendations().isEmpty()) {
      SortedSet<Attribute> recommendedAttributes = new TreeSet<>();
      for (Attribute a : sample.getAttributes()) {
        boolean replaced = false;
        for (AttributeRecommendation rec : recommendation.getAttributeRecommendations()) {
          if (a.getType().equals(rec.getAttribute())) {
            recommendedAttributes.add(
                Attribute.build(
                    rec.getRecommendation(), a.getValue(), a.getTag(), a.getIri(), a.getUnit()));
            replaced = true;
          }
        }
        if (!replaced) {
          recommendedAttributes.add(a);
        }
      }
      recommendedSample =
          Sample.Builder.fromSample(sample).withAttributes(recommendedAttributes).build();
    }
    return recommendedSample;
  }

  private int getSampleQualityScore(Sample sample, int attributeQuality) {
    int quality = attributeQuality;
    if (sample.getExternalReferences() != null && !sample.getExternalReferences().isEmpty()) {
      quality += 15;
    }
    if (sample.getRelationships() != null && !sample.getRelationships().isEmpty()) {
      quality += 15;
    }
    if (sample.getPublications() != null && !sample.getPublications().isEmpty()) {
      quality += 10;
    }
    if (sample.getOrganizations() != null && !sample.getOrganizations().isEmpty()) {
      quality += 5;
    }
    if (sample.getContacts() != null && !sample.getContacts().isEmpty()) {
      quality += 5;
    }

    return quality;
  }
}
