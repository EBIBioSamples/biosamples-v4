package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.AttributeRecommendation;
import uk.ac.ebi.biosamples.model.Recommendation;
import uk.ac.ebi.biosamples.model.Sample;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    private static final String FILE_PATH = "attributes.csv";
    private static final String FILE_PATH_ABBREVIATIONS = "abbreviations.csv";
    private final DataLoader dataLoader;

    public RecommendationService() {
        dataLoader = new DataLoader();
    }

    @PostConstruct
    private void init() {
        dataLoader.loadPopularAttributes(FILE_PATH);
        dataLoader.loadAbbreviations(FILE_PATH_ABBREVIATIONS);
    }

    public Recommendation getRecommendations(Sample sample) {
        SortedSet<String> attributes = new TreeSet<>(sample.getAttributes().stream()
                .map(Attribute::getType).collect(Collectors.toUnmodifiableList()));

        SortedSet<String> goodAttributes = new TreeSet<>(attributes);
        goodAttributes.retainAll(dataLoader.getPopularAttributes());

        SortedSet<String> missingAttributes = new TreeSet<>(attributes);
        missingAttributes.removeAll(dataLoader.getPopularAttributes());
        SortedSet<AttributeRecommendation> badAttributes = new TreeSet<>();
        for (String attr : missingAttributes) {
            badAttributes.add(new AttributeRecommendation.Builder()
                    .withAttribute(attr)
                    .withRecommendations(CuramiUtils.getSimilarAttributes(attr, dataLoader.getPopularAttributes()))
                    .build());
        }

        for (AttributeRecommendation rec : badAttributes) {
            missingAttributes.remove(rec.getAttribute());
        }

        int quality = (100 * goodAttributes.size() - 20 * badAttributes.size() -  30 * missingAttributes.size()) / attributes.size();

        return new Recommendation.Builder().withQuality(quality)
                .withGoodAttributes(goodAttributes)
                .withBadAttributes(badAttributes)
                .withMissingAttributes(missingAttributes)
                .build();
    }
}
