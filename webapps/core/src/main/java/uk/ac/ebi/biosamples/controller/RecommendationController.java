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
package uk.ac.ebi.biosamples.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.core.model.Sample;
import uk.ac.ebi.biosamples.curami.model.CuramiRecommendation;
import uk.ac.ebi.biosamples.curami.model.SampleRecommendation;
import uk.ac.ebi.biosamples.service.RecommendationService;

@RestController
@RequestMapping(value = "/recommendations", produces = MediaType.APPLICATION_JSON_VALUE)
public class RecommendationController {
  private final RecommendationService recommendationService;

  public RecommendationController(final RecommendationService recommendationService) {
    this.recommendationService = recommendationService;
  }

  @PostMapping()
  public SampleRecommendation getRecommendations(@RequestBody final Sample sample) {
    final CuramiRecommendation recommendation = recommendationService.getRecommendations(sample);
    final Sample recommendedSample =
        recommendationService.getRecommendedSample(sample, recommendation);
    return new SampleRecommendation(recommendation, recommendedSample);
  }

  @PostMapping(value = "/attributes")
  public CuramiRecommendation getRecommendedAttributes(@RequestBody final Sample sample) {
    return recommendationService.getRecommendations(sample);
  }

  @PostMapping(value = "/sample")
  public Sample getRecommendedSample(@RequestBody final Sample sample) {
    final CuramiRecommendation recommendation = recommendationService.getRecommendations(sample);
    return recommendationService.getRecommendedSample(sample, recommendation);
  }
}
