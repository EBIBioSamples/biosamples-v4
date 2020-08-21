package uk.ac.ebi.biosamples.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biosamples.model.CuramiRecommendation;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.service.RecommendationService;

import java.util.Map;

@RestController
@RequestMapping(value = "/recommendations", produces = MediaType.APPLICATION_JSON_VALUE)
public class RecommendationRestController {
    private final RecommendationService recommendationService;

    public RecommendationRestController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping()
    public Map<String, Object> getRecommendations(@RequestBody Sample sample) {
        CuramiRecommendation recommendation = recommendationService.getRecommendations(sample);
        Sample recommendedSample = recommendationService.getRecommendedSample(sample, recommendation);
        return Map.of("sample", recommendedSample, "recommendations", recommendation);
    }

    @PostMapping(value = "/attributes")
    public CuramiRecommendation getRecommendedAttributes(@RequestBody Sample sample) {
        return recommendationService.getRecommendations(sample);
    }

    @PostMapping(value = "/sample")
    public Sample getRecommendedSample(@RequestBody Sample sample) {
        CuramiRecommendation recommendation = recommendationService.getRecommendations(sample);
        return recommendationService.getRecommendedSample(sample, recommendation);
    }
}
