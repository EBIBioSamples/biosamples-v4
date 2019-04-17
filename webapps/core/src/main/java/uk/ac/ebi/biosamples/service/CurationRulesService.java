package uk.ac.ebi.biosamples.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.biosamples.model.Attribute;
import uk.ac.ebi.biosamples.model.Curation;
import uk.ac.ebi.biosamples.model.CurationLink;
import uk.ac.ebi.biosamples.model.Sample;
import uk.ac.ebi.biosamples.mongo.model.MongoCurationRule;
import uk.ac.ebi.biosamples.mongo.repo.MongoCurationRuleRepository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CurationRulesService {
    private final Map<String, String> curationRules;
    private static final String CURATION_DOMAIN = "self.curami";

    private MongoCurationRuleRepository repository;
    private CurationPersistService curationPersistService;

    public CurationRulesService(MongoCurationRuleRepository repository,
                                CurationPersistService curationPersistService) {
        this.repository = repository;
        this.curationPersistService = curationPersistService;
        curationRules = loadCurationRulesToMemory();
    }

    public List<CurationLink> getRuleBasedCurations(Sample sample) {
        SortedSet<Attribute> attributes = sample.getAttributes();
        List<CurationLink> curations = new ArrayList<>();
        for (Attribute attribute : attributes) {
            if (curationRules.containsKey(attribute.getType())) {
                Curation curation = Curation.build(
                        Attribute.build(attribute.getType(), attribute.getValue()),
                        Attribute.build(curationRules.get(attribute.getType()), attribute.getValue()));
                CurationLink curationLink = CurationLink.build(sample.getAccession(), curation, CURATION_DOMAIN, Instant.now());
                curations.add(curationLink);
            }
        }

        return curations;
    }

    public Sample getCuratedSample(Sample sample) {
        SortedSet<Attribute> attributes = sample.getAttributes();
        SortedSet<Attribute> curatedAttributes = new TreeSet<>();

        for (Attribute attribute : attributes) {
            if (curationRules.containsKey(attribute.getType())) {
                curatedAttributes.add(Attribute.build(curationRules.get(attribute.getType()), attribute.getValue()));
            } else {
                curatedAttributes.add(attribute);
            }
        }

        return Sample.Builder.fromSample(sample).withAttributes(curatedAttributes).build();
    }

    public Sample saveRuleBasedCurations(Sample sample) {
        saveCurationLinks(sample);
        return getCuratedSample(sample);
    }

    public Map<String, String> getCurationRules() {
        return curationRules;
    }

    public void saveCurationRule(String attributePre, String attributePost) {
        repository.save(MongoCurationRule.build(attributePre, attributePost));
        curationRules.put(attributePre, attributePost);
    }

    private void saveCurationLinks(Sample sample) {
        List<CurationLink> curations = getRuleBasedCurations(sample);
        for (CurationLink curation : curations) {
            curationPersistService.store(curation);
        }
    }

    private Map<String, String> loadCurationRulesToMemory() {
        List<MongoCurationRule> mongoCurationRules = repository.findAll();
        return mongoCurationRules.stream()
                .collect(Collectors.toMap(MongoCurationRule::getAttributePre, MongoCurationRule::getAttributePost));
    }
}
