package uk.ac.ebi.biosamples.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.model.Recommendation;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.RecommendationService;

@RestController
@RequestMapping("/recommendations")
public class RecommendationRestController {
    private final RecommendationService recommendationService;

    public RecommendationRestController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping()
    public Recommendation getRecommendationsForSample(@RequestBody Sample sample) {
        return recommendationService.getRecommendations(sample);
    }
}
